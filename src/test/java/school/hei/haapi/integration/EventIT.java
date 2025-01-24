package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.EventType.COURSE;
import static school.hei.haapi.endpoint.rest.model.EventType.INTEGRATION;
import static school.hei.haapi.endpoint.rest.model.FrequencyScopeDay.MONDAY;
import static school.hei.haapi.endpoint.rest.model.FrequencyScopeDay.WEDNESDAY;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.EVENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.EVENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.createEventCourse1;
import static school.hei.haapi.integration.conf.TestUtils.createIntegrationEvent;
import static school.hei.haapi.integration.conf.TestUtils.event1;
import static school.hei.haapi.integration.conf.TestUtils.event2;
import static school.hei.haapi.integration.conf.TestUtils.event3;
import static school.hei.haapi.integration.conf.TestUtils.expectedCourseEventCreated;
import static school.hei.haapi.integration.conf.TestUtils.expectedIntegrationEventCreated;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.integration.conf.TestUtils.someCreatableEventByManager1;
import static school.hei.haapi.integration.conf.TestUtils.student1AttendEvent2;
import static school.hei.haapi.integration.conf.TestUtils.student1MissEvent1;
import static school.hei.haapi.integration.conf.TestUtils.student2AttendEvent2;
import static school.hei.haapi.integration.conf.TestUtils.student3AttendEvent1;
import static school.hei.haapi.integration.conf.TestUtils.student3MissEvent2;

import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.EventsApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Event;
import school.hei.haapi.endpoint.rest.model.EventParticipant;
import school.hei.haapi.endpoint.rest.model.UpdateEventParticipant;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@Slf4j
@Testcontainers
@AutoConfigureMockMvc
public class EventIT extends FacadeITMockedThirdParties {

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void attempt_to_create_a_frequency_with_missing_data_ko() throws Exception {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Frequency cannot be created without number"
            + " of\"}",
        () ->
            api.crupdateEvents(
                List.of(createEventCourse1(), createIntegrationEvent()),
                MONDAY,
                null,
                "09:00",
                "12:00"));
  }

  @Test
  void attempt_to_create_a_frequency_with_invalid_hour_ko() throws Exception {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Hour must be of format HH:MM\"}",
        () ->
            api.crupdateEvents(
                List.of(createEventCourse1(), createIntegrationEvent()),
                MONDAY,
                2,
                "9:00",
                "12:00"));
  }

  @Test
  void manager_create_event_and_event_participant_by_frequence_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    List<Event> actual =
        api.crupdateEvents(List.of(createEventCourse1()), WEDNESDAY, 3, "08:30", "12:00");

    assertEquals(4, actual.size());
  }

  @Test
  void manager_create_event_and_event_participant_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    List<Event> actual =
        api.crupdateEvents(
            List.of(createEventCourse1(), createIntegrationEvent()), null, null, null, null);

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

    List<EventParticipant> eventParticipantsToUpdate =
        api.getEventParticipants(event2.getId(), 1, 15, null);
  }

  @Test
  void manager_read_event_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    List<Event> actual = api.getEvents(1, 15, null, null, null, null, null);

    System.out.println(actual);
    assertTrue(actual.containsAll(List.of(event1(), event2(), event3())));

    List<Event> eventsBeginAfterAnInstant =
        api.getEvents(1, 15, Instant.parse("2022-12-15T10:00:00.00Z"), null, null, null, null);

    assertTrue(eventsBeginAfterAnInstant.contains(event1()));
    assertFalse(eventsBeginAfterAnInstant.contains(event2()));

    List<Event> eventsBeginBetweenTwoInstant =
        api.getEvents(
            1,
            15,
            Instant.parse("2022-12-07T08:00:00.00Z"),
            Instant.parse("2022-12-10T08:00:00.00Z"),
            null,
            null,
            null);

    assertTrue(eventsBeginBetweenTwoInstant.containsAll(List.of(event2(), event3())));
    assertFalse(eventsBeginBetweenTwoInstant.contains(event1()));

    List<Event> eventsBeginBeforeAnInstant =
        api.getEvents(1, 15, null, Instant.parse("2022-12-08T08:00:00.00Z"), null, null, null);

    assertTrue(eventsBeginBeforeAnInstant.contains(event2()));
    assertFalse(eventsBeginBeforeAnInstant.containsAll(List.of(event1(), event3())));

    List<Event> eventsFilterByType = api.getEvents(1, 15, null, null, COURSE, null, null);
    assertTrue(eventsFilterByType.contains(event1()));
    assertFalse(eventsFilterByType.contains(event3()));
    assertFalse(eventsFilterByType.contains(event2()));

    List<Event> eventsFilterByTitle = api.getEvents(1, 15, null, null, null, "PROG1", null);
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
  void student_create_or_update_event_or_event_participant_ko() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    assertThrowsForbiddenException(
        () -> api.crupdateEvents(List.of(createEventCourse1()), null, null, null, null));
    assertThrowsForbiddenException(
        () -> api.updateEventParticipantsStatus(EVENT1_ID, List.of(new UpdateEventParticipant())));
  }

  @Test
  void delete_event_student_ko_and_manager_ko() throws ApiException {
    EventsApi studentApi = new EventsApi(anApiClient(STUDENT1_TOKEN));
    EventsApi managerApi = new EventsApi(anApiClient(MANAGER1_TOKEN));
    List<Event> events =
        managerApi.crupdateEvents(
            List.of(someCreatableEventByManager1(INTEGRATION)), MONDAY, 1, "09:00", "12:00");

    assertThrowsForbiddenException(() -> studentApi.deleteEventById(events.getFirst().getId()));

    Event deletedEvent = managerApi.deleteEventById(events.getFirst().getId());
    assertEquals(events.getFirst().getId(), deletedEvent.getId());
  }
}
