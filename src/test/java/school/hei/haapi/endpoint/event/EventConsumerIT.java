package school.hei.haapi.endpoint.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import school.hei.haapi.PojaGenerated;
import school.hei.haapi.endpoint.event.consumer.EventConsumer;
import school.hei.haapi.endpoint.event.consumer.model.ConsumableEvent;
import school.hei.haapi.endpoint.event.consumer.model.TypedEvent;
import school.hei.haapi.endpoint.event.model.UuidCreated;
import school.hei.haapi.repository.DummyUuidRepository;

@PojaGenerated
class EventConsumerIT extends school.hei.haapi.integration.conf.FacadeITMockedThirdParties {

  @Autowired EventConsumer subject;
  @Autowired DummyUuidRepository dummyUuidRepository;
  @Autowired ObjectMapper om;

  @Test
  void uuid_created_is_persisted() throws InterruptedException, JsonProcessingException {
    var uuid = randomUUID().toString();
    var uuidCreated = UuidCreated.builder().uuid(uuid).build();
    var payloadReceived = om.readValue(om.writeValueAsString(uuidCreated), UuidCreated.class);

    subject.accept(
        List.of(
            new ConsumableEvent(
                new TypedEvent(
                    "school.hei.haapi.endpoint.event.model.UuidCreated", payloadReceived),
                () -> {},
                () -> {})));

    Thread.sleep(2_000);
    var saved = dummyUuidRepository.findById(uuid).orElseThrow();
    assertEquals(uuid, saved.getId());
  }
}
