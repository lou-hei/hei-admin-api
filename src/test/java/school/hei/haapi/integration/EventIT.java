package school.hei.haapi.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.MISSING;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.PRESENT;
import static school.hei.haapi.endpoint.rest.model.EventType.COURSE;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.EventsApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Event;
import school.hei.haapi.endpoint.rest.model.EventParticipant;
import school.hei.haapi.endpoint.rest.model.UpdateEventParticipant;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = EventIT.ContextInitializer.class)
@AutoConfigureMockMvc
public class EventIT extends MockedThirdParties {

  private static final Logger log = LoggerFactory.getLogger(EventIT.class);

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, EventIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void manager_create_event_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    List<Event> actual =
        api.crupdateEvents(List.of(createEventCourse1(), createIntegrationEvent()));

    Event event = actual.getFirst();
    assertEquals(expectedCourseEventCreated().getType(), event.getType());
    assertEquals(expectedCourseEventCreated().getEndDatetime(), event.getEndDatetime());
    assertEquals(expectedCourseEventCreated().getBeginDatetime(), event.getBeginDatetime());
    assertEquals(expectedCourseEventCreated().getDescription(), event.getDescription());

    Event event2 = actual.getLast();
    assertEquals(expectedIntegrationEventCreated().getType(), event2.getType());
    assertEquals(expectedIntegrationEventCreated().getEndDatetime(), event2.getEndDatetime());
    assertEquals(expectedIntegrationEventCreated().getBeginDatetime(), event2.getBeginDatetime());
    assertEquals(expectedIntegrationEventCreated().getDescription(), event2.getDescription());
  }

  @Test
  void manager_read_event_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    List<Event> actual = api.getEvents(1, 15, null, null, null, null);

    log.info(actual.toString());

    assertTrue(actual.containsAll(List.of(event1(), event2(), event3())));

    List<Event> eventsBeginAfterAnInstant =
        api.getEvents(1, 15, Instant.parse("2022-12-15T10:00:00.00Z"), null, null, null);

    assertTrue(eventsBeginAfterAnInstant.contains(event1()));
    assertFalse(eventsBeginAfterAnInstant.contains(event2()));

    List<Event> eventsBeginBetweenTwoInstant =
        api.getEvents(
            1,
            15,
            Instant.parse("2022-12-07T08:00:00.00Z"),
            Instant.parse("2022-12-10T08:00:00.00Z"),
            null,
            null);

    assertTrue(eventsBeginBetweenTwoInstant.containsAll(List.of(event2(), event3())));
    assertFalse(eventsBeginBetweenTwoInstant.contains(event1()));

    List<Event> eventsBeginBeforeAnInstant =
        api.getEvents(1, 15, null, Instant.parse("2022-12-08T08:00:00.00Z"), null, null);

    assertTrue(eventsBeginBeforeAnInstant.contains(event2()));
    assertFalse(eventsBeginBeforeAnInstant.containsAll(List.of(event1(), event3())));

    List<Event> eventsFilterByType = api.getEvents(1, 15, null, null, COURSE, null);
    assertTrue(eventsFilterByType.contains(event1()));
    assertFalse(eventsFilterByType.contains(event3()));
    assertFalse(eventsFilterByType.contains(event2()));

    List<Event> eventsFilterByTitle = api.getEvents(1, 15, null, null, null, "PROG1");
    assertTrue(eventsFilterByTitle.contains(event1()));
    assertFalse(eventsFilterByTitle.contains(event3()));
    assertFalse(eventsFilterByTitle.contains(event2()));
  }

  @Test
  void manager_read_event_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    Event actual = api.getEventById(EVENT1_ID);

    assertEquals(event1(), actual);
  }

  @Test
  void manager_read_event_participant_ok() throws ApiException {

    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    List<EventParticipant> actual = api.getEventParticipants(EVENT1_ID, 1, 15, null);

    assertTrue(actual.contains(student1MissEvent1()));
    assertTrue(actual.contains(student3AttendEvent1()));
    assertFalse(actual.contains(student1AttendEvent2()));

    List<EventParticipant> participantsFilteredByGroupRef =
        api.getEventParticipants(EVENT2_ID, 1, 15, "G2");

    // Notice :
    // Student 1 and Student 3 are in GROUP 1
    // Student 2 is in GROUP 2

    assertTrue(participantsFilteredByGroupRef.contains(student2AttendEvent2()));
    assertFalse(participantsFilteredByGroupRef.contains(student1AttendEvent2()));
    assertFalse(participantsFilteredByGroupRef.contains(student3MissEvent2()));
  }

  @Test
  @DirtiesContext
  void manager_update_event_participant_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    UpdateEventParticipant updateStudent1StatusEvent1 =
        new UpdateEventParticipant().id(EVENT_PARTICIPANT1_ID).eventStatus(PRESENT);

    UpdateEventParticipant updateStudent3StatusInEvent1 =
        new UpdateEventParticipant().id(EVENT_PARTICIPANT2_ID).eventStatus(MISSING);

    EventParticipant student1IsPresentInEvent1 = student1MissEvent1().eventStatus(PRESENT);
    EventParticipant student3MissEvent1 = student3AttendEvent1().eventStatus(MISSING);

    List<EventParticipant> actual =
        api.updateEventParticipantsStatus(
            EVENT1_ID, List.of(updateStudent1StatusEvent1, updateStudent3StatusInEvent1));

    assertEquals(actual, List.of(student1IsPresentInEvent1, student3MissEvent1));
  }

  @Test
  void student_create_or_update_event_or_event_participant_ko() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    assertThrowsForbiddenException(() -> api.crupdateEvents(List.of(createEventCourse1())));
    assertThrowsForbiddenException(
        () -> api.updateEventParticipantsStatus(EVENT1_ID, List.of(new UpdateEventParticipant())));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
