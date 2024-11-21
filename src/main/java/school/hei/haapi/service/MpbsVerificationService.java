package school.hei.haapi.service;

import static java.util.stream.Collectors.toList;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.FAILED;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.PENDING;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.SUCCESS;
import static school.hei.haapi.endpoint.rest.model.Payment.TypeEnum.MOBILE_MONEY;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import school.hei.haapi.model.Payment;
import school.hei.haapi.repository.MobileTransactionDetailsRepository;
import school.hei.haapi.repository.MpbsRepository;
import school.hei.haapi.repository.MpbsVerificationRepository;

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

  public List<MpbsVerification> checkMobilePaymentThenSaveVerification() {
    List<Mpbs> pendingMpbs = mpbsRepository.findAllByStatus(PENDING);
    log.info("pending mpbs = {}", pendingMpbs.size());
    Instant now = Instant.now();

    return pendingMpbs.stream()
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
