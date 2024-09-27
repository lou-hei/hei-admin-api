package school.hei.haapi.service.utils;

import static school.hei.haapi.service.utils.DataFormatterUtils.instantToCommonDate;

import java.util.List;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Payment;
import school.hei.haapi.model.User;

public class PaidFeeReceiptDataProvider {
  private final User student;
  private final Fee fee;
  private final Payment payment;
  private final List<Payment> paidPaymentsBefore;

  public PaidFeeReceiptDataProvider(
      User student, Fee fee, Payment payment, List<Payment> payments) {
    this.student = student;
    this.fee = fee;
    this.payment = payment;
    this.paidPaymentsBefore = paymentsSinceActual(payments, payment);
  }

  public String getEntirePaymentAuthorName() {
    return student.getLastName() + " " + student.getFirstName();
  }

  public int getFeeTotalAmount() {
    return fee.getTotalAmount();
  }

  public int getTotalPaymentAmount() {
    return payment.getAmount();
  }

  public String getFeeComment() {
    return fee.getComment();
  }

  public int getRemainingAmount() {
    List<Payment> paymentsSinceActual = paymentsSinceActual(paidPaymentsBefore, payment);
    int actualRemainingAmount = defineRemainingAmountSinceActualPayment(paymentsSinceActual, fee);
    return fee.getTotalAmount() - actualRemainingAmount;
  }

  public String getPaymentDate() {
    return instantToCommonDate(payment.getCreationDatetime());
  }

  public school.hei.haapi.endpoint.rest.model.Payment.TypeEnum getPaymentType() {
    return payment.getType();
  }

  private List<Payment> paymentsSinceActual(List<Payment> payments, Payment payment) {
    int endIndex = payments.indexOf(payment);
    return payments.subList(0, endIndex);
  }

  private int defineRemainingAmountSinceActualPayment(List<Payment> payments, Fee fee) {
    int totalFeeAmount = fee.getTotalAmount();
    int actualRemainingAmountSincePayment =
        payments.stream().map(Payment::getAmount).reduce(0, Integer::sum);
    return totalFeeAmount - actualRemainingAmountSincePayment;
  }
}
