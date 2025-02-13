package school.hei.haapi.service;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toUnmodifiableList;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.FAILED;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.PENDING;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.SUCCESS;
import static school.hei.haapi.endpoint.rest.model.Payment.TypeEnum.MOBILE_MONEY;
import static school.hei.haapi.service.utils.DateUtils.convertStringToInstant;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.PaidFeeByMpbsFailedNotificationBody;
import school.hei.haapi.endpoint.rest.model.MpbsStatus;
import school.hei.haapi.http.mapper.ExternalResponseMapper;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.MobileTransactionDetails;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.Mpbs.MpbsVerification;
import school.hei.haapi.model.Mpbs.TypedMobileMoneyTransaction;
import school.hei.haapi.model.Payment;
import school.hei.haapi.repository.MpbsRepository;
import school.hei.haapi.repository.MpbsVerificationRepository;
import school.hei.haapi.service.aws.FileService;

@Service
@AllArgsConstructor
@Slf4j
public class MpbsVerificationService {
  private final MpbsVerificationRepository repository;
  private final MpbsRepository mpbsRepository;
  private final FeeService feeService;
  private final MobilePaymentService mobilePaymentService;
  private final PaymentService paymentService;
  private final UserService userService;
  private final ExternalResponseMapper externalResponseMapper;
  private final EventProducer<PaidFeeByMpbsFailedNotificationBody> eventProducer;
  private final MultipartFileConverter multipartFileConverter;
  private final FileService fileService;

  public List<MpbsVerification> findAllByStudentIdAndFeeId(String studentId, String feeId) {
    return repository.findAllByStudentIdAndFeeId(studentId, feeId);
  }

  @Transactional
  public List<MpbsVerification> verifyMobilePaymentAndSaveResult(
      List<Mpbs> mpbsList, Instant toCompare) {
    log.info("Magic happened here");
    List<MpbsVerification> verifiedMpbs = new ArrayList<>();
    List<Mpbs> unverifiedMpbs = new ArrayList<>();

    // Find all corresponding transaction in database
    List<MobileTransactionDetails> mobileTransactionResponseDetails =
        mobilePaymentService.findAllTransactionByMpbsWithoutException(mpbsList);

    // TIPS: do not use exception to continue script
    for (Mpbs mpbs : mpbsList) {
      List<MobileTransactionDetails> correspondingTransactionDetails =
          mobileTransactionResponseDetails.stream()
              .filter(
                  transactionDetail ->
                      mpbs.getPspId().equals(transactionDetail.getPspTransactionRef()))
              .collect(toUnmodifiableList());

      if (!correspondingTransactionDetails.isEmpty()) {
        MobileTransactionDetails firstCorrespondingTransactionDetails =
            correspondingTransactionDetails.getFirst();
        log.info("mobile transaction found = {}", firstCorrespondingTransactionDetails);
        TransactionDetails transactionDetails =
            externalResponseMapper.toExternalTransactionDetails(
                firstCorrespondingTransactionDetails);
        log.info("mapped transaction details = {}", transactionDetails);
        verifiedMpbs.add(saveTheVerifiedMpbs(mpbs, transactionDetails, toCompare));
      } else {
        unverifiedMpbs.add(mpbs);
      }
    }

    saveTheUnverifiedMpbs(unverifiedMpbs, toCompare);
    return verifiedMpbs;
  }

  @Transactional
  public MpbsVerification verifyMobilePaymentAndSaveResult(Mpbs mpbs, Instant toCompare) {
    List<MpbsVerification> savedPayments =
        verifyMobilePaymentAndSaveResult(List.of(mpbs), toCompare);
    if (savedPayments.isEmpty()) {
      return null;
    }
    return savedPayments.getFirst();
  }

  @Transactional
  public List<Mpbs> computeFromXls(File file) throws IOException {
    List<String> pspToCheck = generateMobileTransactionDetailsFromXlsFile(file);

    List<Mpbs> mpbsToCheck = mpbsRepository.findByPspIdIn(pspToCheck);

    verifyMobilePaymentAndSaveResult(mpbsToCheck, Instant.now());
    return mpbsToCheck;
  }

  public String uploadXlsToS3(MultipartFile multipartFile) {
    String fileKey = "/XLS/" + multipartFile.getOriginalFilename();
    File file = multipartFileConverter.apply(multipartFile);
    fileService.uploadObjectToS3Bucket(fileKey, file);
    return fileKey;
  }

  public Workbook generateWorkBook(File file) throws IOException {
    try {
      return new HSSFWorkbook(new FileInputStream(file));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public List<String> generateMobileTransactionDetailsFromXlsFile(File file) throws IOException {
    log.info("Reading XLS file...");
    List<String> pendingMpbsPspIds =
        mpbsRepository.findAllByStatus(PENDING).stream()
            .map(TypedMobileMoneyTransaction::getPspId)
            .toList();

    List<MobileTransactionDetails> transactions = new ArrayList<>();

    Workbook workbook = generateWorkBook(file);

    Sheet sheet = workbook.getSheetAt(0);

    for (Row row : sheet) {

      Cell dateCell = row.getCell(1);
      Cell timeCell = row.getCell(2);
      Cell refCell = row.getCell(3);
      Cell statusCell = row.getCell(6);
      Cell montantCell = row.getCell(14);

      if (dateCell == null
          || timeCell == null
          || StringUtils.isBlank(dateCell.getStringCellValue())
          || StringUtils.isBlank(timeCell.getStringCellValue())) {
        log.warn("Row {} ignored because of an empty cell", row.getRowNum());
        continue;
      }

      String dateTimeStr =
          dateCell.getStringCellValue().trim() + " " + timeCell.getStringCellValue().trim();
      String ref = refCell.getStringCellValue().trim();

      if (pendingMpbsPspIds.contains(ref)) {

        Instant transactionCreationTime;
        try {
          transactionCreationTime = Instant.from(convertStringToInstant(dateTimeStr));
        } catch (Exception e) {
          log.warn("Failed to parse date/time for row {}: {}", row.getRowNum(), e.getMessage());
          continue;
        }

        MobileTransactionDetails transaction =
            MobileTransactionDetails.builder()
                .id(randomUUID().toString())
                .pspDatetimeTransactionCreation(transactionCreationTime)
                .pspTransactionRef(refCell.getStringCellValue().trim())
                .pspTransactionAmount((int) montantCell.getNumericCellValue())
                .status(
                    MpbsStatus.fromValue(
                        Objects.equals(statusCell.getStringCellValue().trim(), "Succès")
                            ? "SUCCESS"
                            : "FAILED"))
                .pspOwnDatetimeVerification(Instant.now())
                .build();

        transactions.add(transaction);
        log.info("Generated mobile transaction psp id {}", transaction.getPspTransactionRef());
      } else {
        log.info("Unverified mobile transaction psp id {}", ref);
      }
    }
    mobilePaymentService.saveAll(transactions);
    log.info("Verification done...");
    return transactions.stream()
        .map(MobileTransactionDetails::getPspTransactionRef)
        .collect(Collectors.toList());
  }

  private List<Mpbs> saveTheUnverifiedMpbs(List<Mpbs> mpbsList, Instant toCompare) {
    mpbsList.forEach(
        mpbs -> {
          mpbs.setLastVerificationDatetime(Instant.now());
          mpbs.setStatus(defineMpbsStatusWithoutOrangeTransactionDetails(mpbs, toCompare));
        });

    List<Mpbs> failedMpbs =
        mpbsList.stream()
            .filter(mpbs -> FAILED.equals(mpbs.getStatus()))
            .collect(toUnmodifiableList());

    notifyStudentForFailedPayment(failedMpbs);
    return mpbsRepository.saveAll(mpbsList);
  }

  private MpbsVerification saveTheVerifiedMpbs(
      Mpbs mpbs, TransactionDetails correspondingMobileTransaction, Instant toCompare) {
    Instant now = Instant.now();
    Fee fee = mpbs.getFee();
    MpbsVerification verifiedMobileTransaction =
        MpbsVerification.builder()
            .amountInPsp(correspondingMobileTransaction.getPspTransactionAmount())
            .fee(fee)
            .amountOfFeeRemainingPayment(fee.getRemainingAmount())
            .creationDatetimeOfMpbs(mpbs.getCreationDatetime())
            .creationDatetimeOfPaymentInPsp(
                correspondingMobileTransaction.getPspDatetimeTransactionCreation())
            .student(mpbs.getStudent())
            .build();

    // Update mpbs ...
    mpbs.setSuccessfullyVerifiedOn(now);
    mpbs.setStatus(defineMpbsStatusFromOrangeTransactionDetails(correspondingMobileTransaction));
    mpbs.setPspOwnDatetimeVerification(
        correspondingMobileTransaction.getPspOwnDatetimeVerification());
    var successfullyVerifiedMpbs = mpbsRepository.save(mpbs);
    log.info("Mpbs has successfully verified = {}", mpbs.toString());

    // ... then save the verification
    verifiedMobileTransaction.setMobileMoneyType(successfullyVerifiedMpbs.getMobileMoneyType());
    verifiedMobileTransaction.setPspId(successfullyVerifiedMpbs.getPspId());
    repository.save(verifiedMobileTransaction);

    // ... then save the corresponding payment
    paymentService.savePaymentFromMpbs(
        successfullyVerifiedMpbs, correspondingMobileTransaction.getPspTransactionAmount());
    log.info("Creating corresponding payment = {}", successfullyVerifiedMpbs.toString());

    // ... then update student status
    paymentService.computeUserStatusAfterPayingFee(mpbs.getStudent());
    log.info(
        "Student computed status: {}",
        (userService.findById(mpbs.getStudent().getId())).getStatus());

    // ... then update fee remaining amount
    feeService.debitAmountFromMpbs(fee, verifiedMobileTransaction.getAmountInPsp());

    return verifiedMobileTransaction;
  }

  @Transactional
  public void checkMobilePaymentThenSaveVerification() {
    List<Mpbs> pendingMpbs = mpbsRepository.findAllByStatus(PENDING);
    log.info("pending mpbs = {}", pendingMpbs.size());
    Instant now = Instant.now();

    verifyMobilePaymentAndSaveResult(pendingMpbs, now);
  }

  public List<TransactionDetails> fetchThenSaveTransactionDetailsDaily() {
    return mobilePaymentService.fetchThenSaveTransactionDetails();
  }

  private MpbsStatus defineMpbsStatusFromOrangeTransactionDetails(
      TransactionDetails storedTransaction) {

    // 1. if it contains and if the status is success then make it success
    if (SUCCESS.equals(storedTransaction.getStatus())) {
      log.info("correct");
      return SUCCESS;
    }
    // 2. and else if the mpbs is stored to day or less than 2 days, it will be verified later
    log.info("status computed = else");
    return PENDING;
  }

  private MpbsStatus defineMpbsStatusWithoutOrangeTransactionDetails(Mpbs mpbs, Instant toCompare) {
    long dayValidity = mpbs.getCreationDatetime().until(toCompare, DAYS);
    if (dayValidity > 2) {
      log.info("failed transaction");
      return FAILED;
    }
    log.info("pending transaction");
    return PENDING;
  }

  private void notifyStudentForFailedPayment(List<Mpbs> mpbsList) {
    List<PaidFeeByMpbsFailedNotificationBody> notifications = new ArrayList<>();

    mpbsList.forEach(
        mpbs -> {
          Payment paymentFromMpbs = getPaymentFromMpbs(mpbs);
          log.info("Fail verification {} for student {}", mpbs.getId(), mpbs.getStudent().getId());
          notifications.add(PaidFeeByMpbsFailedNotificationBody.from(paymentFromMpbs));
        });
    eventProducer.accept(notifications);
  }

  private Payment getPaymentFromMpbs(Mpbs mpbs) {
    Fee correspondingFee = mpbs.getFee();
    return Payment.builder()
        .type(MOBILE_MONEY)
        .fee(correspondingFee)
        .amount(mpbs.getAmount())
        .creationDatetime(Instant.now())
        .comment(correspondingFee.getComment())
        .build();
  }
}
