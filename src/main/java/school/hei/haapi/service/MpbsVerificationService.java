package school.hei.haapi.service;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import school.hei.haapi.endpoint.event.model.PojaEvent;
import school.hei.haapi.endpoint.rest.model.MpbsStatus;
import school.hei.haapi.http.mapper.ExternalResponseMapper;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.MobileTransactionDetails;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.Mpbs.MpbsVerification;
import school.hei.haapi.model.Mpbs.TypedMobileMoneyTransaction;
import school.hei.haapi.model.Payment;
import school.hei.haapi.repository.MobileTransactionDetailsRepository;
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
  private final MobileTransactionDetailsRepository mobileTransactionDetailsRepository;
  private final EventProducer<PojaEvent> eventProducer;
  private final MultipartFileConverter multipartFileConverter;
  private final FileService fileService;

  public List<MpbsVerification> findAllByStudentIdAndFeeId(String studentId, String feeId) {
    return repository.findAllByStudentIdAndFeeId(studentId, feeId);
  }

  @Transactional
  public MpbsVerification verifyMobilePaymentAndSaveResult(Mpbs mpbs, Instant toCompare) {
    log.info("Magic happened here");
    // Find transaction in database
    Optional<MobileTransactionDetails> mobileTransactionResponseDetails =
        mobilePaymentService.findTransactionByMpbsWithoutException(mpbs);

    // TIPS: do not use exception to continue script
    if (mobileTransactionResponseDetails.isPresent()) {
      log.info("mobile transaction found = {}", mobileTransactionResponseDetails.get());
      TransactionDetails transactionDetails =
          externalResponseMapper.toExternalTransactionDetails(
              mobileTransactionResponseDetails.get());
      log.info("mapped transaction details = {}", transactionDetails);
      return saveTheVerifiedMpbs(mpbs, transactionDetails, toCompare);
    }
    log.info("mobile transaction not found");
    saveTheUnverifiedMpbs(mpbs, toCompare);
    return null;
  }

  @Transactional
  public List<Mpbs> computeFromXls(File file) throws IOException {
    List<String> pspToCheck = generateMobileTransactionDetailsFromXlsFile(file);

    List<Mpbs> mpbsToCheck = mpbsRepository.findByPspIdIn(pspToCheck);

    List<Mpbs> mpbsToSave = new ArrayList<>();

    for (Mpbs mpbs : mpbsToCheck) {
      log.info("mpbs to update = {}", mpbs);
      verifyMobilePaymentAndSaveResult(mpbs, Instant.now());
      mpbsToSave.add(mpbs);
    }

    return mpbsToSave;
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
      if (row.getRowNum() < 26) continue;

      Cell dateCell = row.getCell(1);
      Cell timeCell = row.getCell(2);
      Cell refCell = row.getCell(3);
      Cell statusCell = row.getCell(6);
      Cell montantCell = row.getCell(14);

      if (dateCell == null
          || timeCell == null
          || StringUtils.isBlank(dateCell.getStringCellValue())
          || StringUtils.isBlank(timeCell.getStringCellValue())) {
        log.warn("Row ignored because of an empty cell");
        continue;
      }

      String dateTimeStr =
          dateCell.getStringCellValue().trim() + " " + timeCell.getStringCellValue().trim();
      String ref = refCell.getStringCellValue().trim();

      if (pendingMpbsPspIds.contains(ref)) {
        MobileTransactionDetails transaction =
            MobileTransactionDetails.builder()
                .id(randomUUID().toString())
                .pspDatetimeTransactionCreation(Instant.from(convertStringToInstant(dateTimeStr)))
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
      }
      ;
    }
    mobilePaymentService.saveAll(transactions);
    log.info("Verification done...");
    return transactions.stream()
        .map(MobileTransactionDetails::getPspTransactionRef)
        .collect(Collectors.toList());
  }

  private Mpbs saveTheUnverifiedMpbs(Mpbs mpbs, Instant toCompare) {
    mpbs.setLastVerificationDatetime(Instant.now());
    mpbs.setStatus(defineMpbsStatusWithoutOrangeTransactionDetails(mpbs, toCompare));
    return mpbsRepository.save(mpbs);
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

  public void checkMobilePaymentThenSaveVerification() {
    List<Mpbs> pendingMpbs = mpbsRepository.findAllByStatus(PENDING);
    log.info("pending mpbs = {}", pendingMpbs.size());
    Instant now = Instant.now();

    pendingMpbs.stream()
        .map((mpbs -> this.verifyMobilePaymentAndSaveResult(mpbs, now)))
        .collect(toList());
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
    long dayValidity = mpbs.getCreationDatetime().until(toCompare, ChronoUnit.DAYS);
    if (dayValidity > 2) {
      // notifyStudentForFailedPayment(mpbs);
      log.info("failed transaction");
      return FAILED;
    }
    log.info("pending transaction");
    return PENDING;
  }

  private void notifyStudentForFailedPayment(Mpbs mpbs) {
    Fee correspondingFee = mpbs.getFee();
    Payment paymentFromMpbs =
        Payment.builder()
            .type(MOBILE_MONEY)
            .fee(correspondingFee)
            .amount(mpbs.getAmount())
            .creationDatetime(Instant.now())
            .comment(correspondingFee.getComment())
            .build();
    PaidFeeByMpbsFailedNotificationBody notificationBody =
        PaidFeeByMpbsFailedNotificationBody.from(paymentFromMpbs);
    eventProducer.accept(List.of(notificationBody));
    log.info(
        "Failed payment notification for user {} sent to Queue.",
        notificationBody.getMpbsAuthorEmail());
  }
}
