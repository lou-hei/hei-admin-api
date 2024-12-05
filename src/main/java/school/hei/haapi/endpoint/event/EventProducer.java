package school.hei.haapi.endpoint.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import school.hei.haapi.PojaGenerated;
import school.hei.haapi.datastructure.ListGrouper;
import school.hei.haapi.endpoint.event.model.PojaEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

@PojaGenerated
@Component
@Slf4j
public class EventProducer<T extends PojaEvent> implements Consumer<Collection<T>> {
  private final ObjectMapper om;
  private final EventBridgeClient eventBridgeClient;
  private final String eventBusName;

  private static final int MAX_EVENTS_FOR_PUT_REQUEST = 10;
  private final ListGrouper<T> listGrouper;

  public EventProducer(
      ObjectMapper om,
      EventBridgeClient eventBridgeClient,
      @Value("${aws.eventBridge.bus}") String eventBusName,
      ListGrouper<T> listGrouper) {
    this.om = om;
    this.eventBridgeClient = eventBridgeClient;
    this.listGrouper = listGrouper;
    this.eventBusName = eventBusName;
  }

  @Override
  public void accept(Collection<T> events) {
    for (var batch : listGrouper.apply(events.stream().toList(), MAX_EVENTS_FOR_PUT_REQUEST)) {
      log.info("Events to send: {}", batch);
      PutEventsResponse response = sendRequest(batch);
      checkResponse(response);
    }
  }

  private PutEventsRequest toEventsRequest(List<T> events) {
    return PutEventsRequest.builder()
        .entries(events.stream().map(this::toRequestEntry).toList())
        .build();
  }

  private PutEventsRequestEntry toRequestEntry(PojaEvent event) {
    try {
      String eventAsString = om.writeValueAsString(event);
      return PutEventsRequestEntry.builder()
          .source(event.getEventSource())
          .detailType(event.getClass().getTypeName())
          .detail(eventAsString)
          .eventBusName(this.eventBusName)
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private PutEventsResponse sendRequest(List<T> events) {
    checkPayload(events);
    PutEventsRequest eventsRequest = toEventsRequest(events);
    return eventBridgeClient.putEvents(eventsRequest);
  }

  private boolean isPayloadValid(List<T> events) {
    PutEventsRequest eventsRequest = toEventsRequest(events);
    return eventsRequest.entries().size() <= Conf.MAX_PUT_EVENT_ENTRIES;
  }

  private void checkPayload(List<T> events) {
    if (!isPayloadValid(events)) {
      throw new RuntimeException("Request entries must be <= " + Conf.MAX_PUT_EVENT_ENTRIES);
    }
  }

  private void checkResponse(PutEventsResponse response) {
    List<PutEventsResultEntry> failedEntries = new ArrayList<>();
    List<PutEventsResultEntry> successfulEntries = new ArrayList<>();

    for (PutEventsResultEntry resultEntry : response.entries()) {
      if (resultEntry.eventId() == null) {
        failedEntries.add(resultEntry);
      }
      successfulEntries.add(resultEntry);
    }

    if (!failedEntries.isEmpty()) {
      log.error("Following events were not successfully sent: {}", failedEntries);
    }
    if (!successfulEntries.isEmpty()) {
      log.info("Following events were successfully sent: {}", successfulEntries);
    }
  }

  @Configuration
  public static class Conf {
    public static final int MAX_PUT_EVENT_ENTRIES = 10;
    private final Region region;

    public Conf(@Value("eu-west-3") String region) {
      this.region = Region.of(region);
    }

    @Bean
    public EventBridgeClient getEventBridgeClient() {
      return EventBridgeClient.builder().region(region).build();
    }
  }
}
