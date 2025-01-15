package school.hei.haapi.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@ToString
@Getter
public class SendLetterEmail extends PojaEvent {

  @JsonProperty("id")
  private String id;

  @JsonProperty("studentRef")
  private String studentRef;

  @JsonProperty("studentEmail")
  private String studentEmail;

  @JsonProperty("description")
  private String description;

  @JsonProperty("receiver")
  private String receiver;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(60);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60);
  }
}
