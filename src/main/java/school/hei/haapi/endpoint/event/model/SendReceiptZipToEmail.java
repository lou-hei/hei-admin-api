package school.hei.haapi.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@ToString
@Getter
public class SendReceiptZipToEmail extends PojaEvent {

  @JsonProperty("startRequest")
  private Instant startRequest;

  @JsonProperty("fileToZip")
  private List<byte[]> fileToZip;

  @JsonProperty("emailRecipient")
  private String emailRecipient;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(60);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60);
  }
}
