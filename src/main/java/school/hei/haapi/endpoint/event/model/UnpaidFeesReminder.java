package school.hei.haapi.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import school.hei.haapi.model.User;

@EqualsAndHashCode
@Builder
@ToString
@AllArgsConstructor
@Data
public class UnpaidFeesReminder extends PojaEvent {
  @JsonProperty("concerned_student")
  UnpaidFeesUser user;

  @JsonProperty("remainingAmount")
  private Integer remainingAmount;

  @JsonProperty("dueDatetime")
  private Instant dueDatetime;

  @JsonProperty("comment")
  private String comment;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }

  public record UnpaidFeesUser(
      String id, String ref, String lastName, String firstName, String email) {
    public static UnpaidFeesUser from(User user) {
      return new UnpaidFeesUser(
          user.getId(), user.getRef(), user.getLastName(), user.getFirstName(), user.getEmail());
    }
  }
  ;
}
