package school.hei.haapi.endpoint.event.consumer.model;

import static java.lang.Integer.parseInt;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import school.hei.haapi.PojaGenerated;
import school.hei.haapi.endpoint.event.EventConf;
import school.hei.haapi.endpoint.event.model.PojaEvent;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

@PojaGenerated
@Component
@Slf4j
@AllArgsConstructor
public class ConsumableEventTyper implements Function<List<SQSMessage>, List<ConsumableEvent>> {

  private static final String DETAIL_PROPERTY = "detail";
  private static final String DETAIL_TYPE_PROPERTY = "detail-type";
  private static final String SQS_APPROXIMATE_RECEIVE_COUNT_SQS_ATTRIBUTE =
      "ApproximateReceiveCount";

  private final ObjectMapper om;
  private final EventConf eventConf;

  @Override
  public List<ConsumableEvent> apply(List<SQSMessage> messages) {
    var res = new ArrayList<ConsumableEvent>();
    for (SQSMessage message : messages) {
      TypedEvent typedEvent;
      try {
        typedEvent = toTypedEvent(message);
      } catch (Exception e) {
        log.error(e.getMessage());
        log.error("Message could not be unmarshalled, message : {} \n", message);
        continue;
      }
      String sqsQueueUrl = typedEvent.payload().getEventStack().getSqsQueueUrl();
      ConsumableEvent consumableEvent =
          new ConsumableEvent(
              typedEvent,
              acknowledger(message, sqsQueueUrl),
              visibilityChanger(message, sqsQueueUrl));
      res.add(consumableEvent);
    }
    return res;
  }

  @SneakyThrows
  private TypedEvent toTypedEvent(SQSMessage message) {
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
    Map<String, Object> body = om.readValue(message.getBody(), typeRef);
    String typeName = body.get(DETAIL_TYPE_PROPERTY).toString();
    var pojaEvent = (PojaEvent) om.convertValue(body.get(DETAIL_PROPERTY), Class.forName(typeName));
    var sqsMessageAttributes = message.getAttributes();
    pojaEvent.setAttemptNb(
        parseInt(sqsMessageAttributes.get(SQS_APPROXIMATE_RECEIVE_COUNT_SQS_ATTRIBUTE)));

    return new TypedEvent(typeName, pojaEvent);
  }

  private Runnable acknowledger(SQSMessage message, String sqsQueueUrl) {
    return () -> {
      sqsClient()
          .deleteMessage(
              DeleteMessageRequest.builder()
                  .queueUrl(sqsQueueUrl)
                  .receiptHandle(message.getReceiptHandle())
                  .build());
      log.info("deleted message: {}", message);
    };
  }

  private Runnable visibilityChanger(SQSMessage message, String sqsQueueUrl) {
    return () -> {
      var newRandomVisibility =
          (int) (toTypedEvent(message).payload()).randomVisibilityTimeout().toSeconds();
      sqsClient()
          .changeMessageVisibility(
              ChangeMessageVisibilityRequest.builder()
                  .queueUrl(sqsQueueUrl)
                  .receiptHandle(message.getReceiptHandle())
                  .visibilityTimeout(newRandomVisibility)
                  .build());
      log.info("newVisibility={}, message={}", newRandomVisibility, message);
    };
  }

  private SqsClient sqsClient() {
    return eventConf.getSqsClient();
  }
}
