package school.hei.haapi.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import school.hei.haapi.model.EventParticipant;

@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class MissedEventEmail extends PojaEvent {

  @JsonProperty("event_id")
  private String eventId;

  @JsonProperty("student_id")
  private String studentId;

  @JsonProperty("student_email")
  private String studentEmail;

  public static MissedEventEmail fromEventParticipant(EventParticipant eventParticipant) {
    return MissedEventEmail.builder()
        .eventId(eventParticipant.getEvent().getId())
        .studentEmail(eventParticipant.getParticipant().getEmail())
        .studentId(eventParticipant.getParticipant().getId())
        .build();
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(60);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60);
  }
}
