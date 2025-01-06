package school.hei.haapi.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Payment;
import school.hei.haapi.model.User;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@Data
@ToString
public class PaidFeeByMpbsNotificationBody extends PojaEvent {
  @JsonProperty("mpbs_author")
  private String mpbsAuthor;

  @JsonProperty("mpbs_author_email")
  private String mpbsAuthorEmail;

  @JsonProperty("amount")
  private int amount;

  @JsonProperty("fee_id")
  private String feeId;

  public static PaidFeeByMpbsNotificationBody from(Payment payment) {
    return PaidFeeByMpbsNotificationBody.builder()
        .amount(payment.getAmount())
        .mpbsAuthor(defineMpbsAuthorEntireName(payment.getFee()))
        .mpbsAuthorEmail(payment.getFee().getStudent().getEmail())
        .feeId(payment.getFee().getId())
        .build();
  }

  private static String defineMpbsAuthorEntireName(Fee fee) {
    User student = fee.getStudent();
    return student.getLastName() + " " + student.getFirstName();
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
