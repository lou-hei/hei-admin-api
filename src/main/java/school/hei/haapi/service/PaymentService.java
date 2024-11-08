package school.hei.haapi.service;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.UNPAID;
import static school.hei.haapi.endpoint.rest.model.Payment.TypeEnum.MOBILE_MONEY;
import static school.hei.haapi.model.User.Status.ENABLED;
import static school.hei.haapi.model.User.Status.SUSPENDED;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.PaidFeeByMpbsNotificationBody;
import school.hei.haapi.endpoint.event.model.SuspensionEndedEmailBody;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.Payment;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.model.validator.PaymentValidator;
import school.hei.haapi.repository.FeeRepository;
import school.hei.haapi.repository.PaymentRepository;
import school.hei.haapi.repository.UserRepository;
import school.hei.haapi.repository.dao.UserManagerDao;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {
  private final FeeRepository feeRepository;
  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final FeeService feeService;
  private final UserManagerDao userManagerDao;
  private final PaymentValidator paymentValidator;
  private final EventProducer eventProducer;

  public Payment deleteFeePaymentById(String paymentId) {
    Payment deletedPayment = getById(paymentId);
    Fee associatedFee = deletedPayment.getFee();

    if (deletedPayment.isDeleted()) {
      throw new BadRequestException("Payment with id #" + paymentId + " already deleted");
    }

    resetRemainingAmountBetweenDelete(associatedFee, deletedPayment.getAmount());
    paymentRepository.deleteById(paymentId);
    return deletedPayment;
  }

  private void resetRemainingAmountBetweenDelete(Fee associatedFee, int amount) {
    Instant now = Instant.now();
    associatedFee.setRemainingAmount(associatedFee.getRemainingAmount() + amount);

    if (associatedFee.getDueDatetime().isBefore(now) && associatedFee.getRemainingAmount() != 0) {
      associatedFee.setStatus(LATE);
    }
    if (associatedFee.getDueDatetime().isAfter(now) && associatedFee.getRemainingAmount() != 0) {
      associatedFee.setStatus(UNPAID);
    }
    feeRepository.save(associatedFee);
  }

  public Payment getById(String paymentId) {
    return paymentRepository
        .findById(paymentId)
        .orElseThrow(() -> new NotFoundException("Payment with id: " + paymentId + " not found"));
  }

  public List<Payment> getByStudentIdAndFeeId(
      String studentId, String feeId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(DESC, "creationDatetime"));
    return paymentRepository.getByStudentIdAndFeeIdWithPagination(studentId, feeId, pageable);
  }

  public List<Payment> getByFeeIdOrderByCreationDatetimeAsc(String feeId) {
    return paymentRepository.findAllByFee_IdOrderByCreationDatetimeAsc(feeId);
  }

  List<Payment> getByStudentIdAndFeeId(String studentId, String feeId) {
    return paymentRepository.getByStudentIdAndFeeId(studentId, feeId);
  }

  @Transactional
  public void computeRemainingAmount(String feeId, int amount) {
    Fee associatedFee = feeService.getById(feeId);
    User student = associatedFee.getStudent();
    // Array to hold the student's status before and after the payment
    User.Status[] studentStatusBetweenPayingFee = new User.Status[2];
    studentStatusBetweenPayingFee[0] = student.getStatus();

    associatedFee.setRemainingAmount(associatedFee.getRemainingAmount() - amount);
    computeUserStatusAfterPayingFee(student);
    studentStatusBetweenPayingFee[1] = student.getStatus();
    log.info("User status is computed to {}", studentStatusBetweenPayingFee[1]);

    // If the student's status changes from SUSPENDED to ENABLED, send a notification
    if (SUSPENDED.equals(studentStatusBetweenPayingFee[0])
        && ENABLED.equals(studentStatusBetweenPayingFee[1])) {
      notifyStudentForEnabling(associatedFee, amount);
    }
    if (associatedFee.getRemainingAmount() == 0) {
      associatedFee.setStatus(PAID);
    }
  }

  private void notifyStudentForEnabling(Fee associatedFee, int amount) {
    // Build the payment object without unnecessary fields (type and comment fields are omitted
    // since they are not used in the SuspensionEndedEmailBody)
    Payment payment =
        Payment.builder().fee(associatedFee).amount(amount).creationDatetime(Instant.now()).build();
    SuspensionEndedEmailBody suspensionEndedEmailBody = SuspensionEndedEmailBody.from(payment);
    eventProducer.accept(List.of(suspensionEndedEmailBody));
    log.info(
        "End of suspension notification for user {} sent to Queue.",
        suspensionEndedEmailBody.getMpbsAuthorEmail());
  }

  @Transactional
  public void computeUserStatusAfterPayingFee(User userToResetStatus) {
    Instant now = Instant.now();
    List<Fee> unpaidFeesBeforeNow =
        feeRepository.getStudentFeesUnpaidOrLateFrom(now, userToResetStatus.getId(), LATE);
    log.info("unpaid student fees size = {}", unpaidFeesBeforeNow.size());
    log.info("user corresponding = {}", userToResetStatus.toString());
    if (!unpaidFeesBeforeNow.isEmpty()) {
      log.info("SUSPENDED");
      userManagerDao.updateUserStatusById(SUSPENDED, userToResetStatus.getId());
    } else {
      log.info("RE - ENABLED");
      userManagerDao.updateUserStatusById(ENABLED, userToResetStatus.getId());
    }
  }

  @Transactional
  public Payment savePaymentFromMpbs(Mpbs verifiedMpbs, int amount) {
    Fee correspondingFee = verifiedMpbs.getFee();
    Payment paymentFromMpbs =
        Payment.builder()
            .type(MOBILE_MONEY)
            .fee(correspondingFee)
            .amount(amount)
            .creationDatetime(Instant.now())
            .comment(correspondingFee.getComment())
            .build();
    eventProducer.accept(List.of(PaidFeeByMpbsNotificationBody.from(paymentFromMpbs)));
    return paymentRepository.save(paymentFromMpbs);
  }

  @Transactional
  public List<Payment> saveAll(List<Payment> toCreate) {
    paymentValidator.accept(toCreate);
    toCreate.forEach(
        payment -> computeRemainingAmount(payment.getFee().getId(), payment.getAmount()));
    return paymentRepository.saveAll(toCreate);
  }
}
