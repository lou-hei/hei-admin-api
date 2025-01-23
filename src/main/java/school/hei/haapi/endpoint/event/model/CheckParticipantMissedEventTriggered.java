package school.hei.haapi.endpoint.event.model;

import java.time.Duration;

public class CheckParticipantMissedEventTriggered extends PojaEvent {
  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(300);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
