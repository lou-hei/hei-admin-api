package school.hei.haapi.integration;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.EventType.COURSE;
import static school.hei.haapi.endpoint.rest.model.EventType.INTEGRATION;
import static school.hei.haapi.endpoint.rest.model.FrequencyScopeDay.MONDAY;
import static school.hei.haapi.endpoint.rest.model.FrequencyScopeDay.WEDNESDAY;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.StudentIT.student2;
import static school.hei.haapi.integration.conf.TestUtils.ADMIN1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.EVENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.EVENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
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
import static school.hei.haapi.integration.conf.TestUtils.someCreatableEvent;
import static school.hei.haapi.integration.conf.TestUtils.someCreatableEventByManager1;
import static school.hei.haapi.integration.conf.TestUtils.student1AttendEvent2;
import static school.hei.haapi.integration.conf.TestUtils.student1MissEvent1;
import static school.hei.haapi.integration.conf.TestUtils.student2AttendEvent2;
import static school.hei.haapi.integration.conf.TestUtils.student3AttendEvent1;
import static school.hei.haapi.integration.conf.TestUtils.student3MissEvent2;

import java.time.Duration;
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
import school.hei.haapi.endpoint.rest.model.AttendanceStatus;
import school.hei.haapi.endpoint.rest.model.CreateEvent;
import school.hei.haapi.endpoint.rest.model.Event;
import school.hei.haapi.endpoint.rest.model.EventParticipant;
import school.hei.haapi.endpoint.rest.model.EventParticipantStats;
import school.hei.haapi.endpoint.rest.model.EventStats;
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

    CreateEvent eventCourse1 = createEventCourse1();

    List<Event> notSortedActual =
        api.crupdateEvents(List.of(eventCourse1), WEDNESDAY, 3, "08:30", "12:00");
    assertEquals(3, notSortedActual.size());

    // Sort the result for better readability in the test
    List<Event> actual =
        notSortedActual.stream()
            .sorted(comparing(Event::getBeginDatetime))
            .collect(toUnmodifiableList());

    Event eventWeek1 = actual.getFirst();
    assertEquals(Instant.parse("2023-12-13T08:30:00Z"), eventWeek1.getBeginDatetime());
    assertEquals(Instant.parse("2023-12-13T12:00:00Z"), eventWeek1.getEndDatetime());

    Event eventWeek2 = actual.get(1);
    assertEquals(Instant.parse("2023-12-20T08:30:00Z"), eventWeek2.getBeginDatetime());
    assertEquals(Instant.parse("2023-12-20T12:00:00Z"), eventWeek2.getEndDatetime());

    Event eventWeek3 = actual.get(2);
    assertEquals(Instant.parse("2023-12-27T08:30:00Z"), eventWeek3.getBeginDatetime());
    assertEquals(Instant.parse("2023-12-27T12:00:00Z"), eventWeek3.getEndDatetime());
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

    List<EventParticipant> actualEventParticipant0 =
        api.getEventParticipants(event2.getId(), 1, 15, null, null, null, null);
    assertEquals(3, actualEventParticipant0.size());

    // Assert that participant is not duplicated
    api.crupdateEvents(
        List.of(createEventCourse1(), createIntegrationEvent()), null, null, null, null);
    List<EventParticipant> actualEventParticipant1 =
        api.getEventParticipants(event2.getId(), 1, 15, null, null, null, null);
    assertEquals(3, actualEventParticipant1.size());
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

    List<EventParticipant> actual =
        api.getEventParticipants(EVENT1_ID, 1, 15, null, null, null, null);

    assertTrue(actual.contains(student1MissEvent1()));
    assertTrue(actual.contains(student3AttendEvent1()));
    assertFalse(actual.contains(student1AttendEvent2()));

    List<EventParticipant> participantsFilteredByGroupRef =
        api.getEventParticipants(EVENT2_ID, 1, 15, "G2", null, null, null);

    // Notice :
    // Student 1 and Student 3 are in GROUP 1
    // Student 2 is in GROUP 2

    assertTrue(participantsFilteredByGroupRef.contains(student2AttendEvent2()));
    assertFalse(participantsFilteredByGroupRef.contains(student1AttendEvent2()));
    assertFalse(participantsFilteredByGroupRef.contains(student3MissEvent2()));
  }

  @Test
  void manager_read_event_participant_with_criteria_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    EventsApi api = new EventsApi(apiClient);

    // Notice :
    // Student 1 and Student 3 are in GROUP 1
    // Student 2 is in GROUP 2

    // Test the ref filter

    List<EventParticipant> participantsFilteredByRef =
        api.getEventParticipants(EVENT2_ID, 1, 15, null, student2().getRef(), null, null);

    assertEquals(participantsFilteredByRef.getFirst(), student2AttendEvent2());

    // Test the name filter

    List<EventParticipant> participantsFilteredByName =
        api.getEventParticipants(EVENT2_ID, 1, 15, null, null, student2().getLastName(), null);

    assertTrue(participantsFilteredByName.contains(student2AttendEvent2()));

    // Test the status filter

    List<EventParticipant> participantsFilteredByStatus =
        api.getEventParticipants(EVENT2_ID, 1, 15, null, null, null, AttendanceStatus.MISSING);

    assertEquals(participantsFilteredByStatus.getFirst(), student3MissEvent2());
  }

  @Test
  void student_create_or_update_event_or_event_participant_ko() {
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

  @Test
  void student_get_event_stats_ko() {
    EventsApi studentApi = new EventsApi(anApiClient(STUDENT1_TOKEN));
    assertThrowsForbiddenException(() -> studentApi.getEventStats(null, null, null));
  }

  @Test
  void manager_get_overall_stats_ok() {
    EventsApi managerApi = new EventsApi(anApiClient(MANAGER1_TOKEN));
    assertDoesNotThrow(() -> managerApi.getEventStats(null, null, null));
  }

  @Test
  void admin_get_overall_stats_ok() {
    EventsApi managerApi = new EventsApi(anApiClient(ADMIN1_TOKEN));
    assertDoesNotThrow(() -> managerApi.getEventStats(null, null, null));
  }

  @Test
  void event_stats_are_exact() throws ApiException {
    EventsApi managerApi = new EventsApi(anApiClient(MANAGER1_TOKEN));
    List<Event> createdEvents =
        managerApi.crupdateEvents(
            List.of(
                someCreatableEvent(
                    COURSE, MANAGER_ID, Instant.now(), Instant.now().plus(Duration.of(4, HOURS)))),
            null,
            null,
            null,
            null);

    Event createdEvent = createdEvents.getFirst();

    EventStats actualEventStats = managerApi.getEventStats(createdEvent.getId(), null, null);

    // Notice :
    // Student 1 and Student 3 are in GROUP 1
    EventStats expectedEventStats = new EventStats().late(0).present(0).missing(2).total(2);

    assertEquals(expectedEventStats, actualEventStats);
  }

  @Test
  void student_get_stats_ko() {
    EventsApi studentApi = new EventsApi(anApiClient(STUDENT1_TOKEN));

    assertThrowsForbiddenException(
        () -> studentApi.getEventParticipantStats(STUDENT1_ID, null, null));
  }

  @Test
  void get_stats_ok() throws ApiException {
    EventsApi managerApi = new EventsApi(anApiClient(MANAGER1_TOKEN));
    // TODO: create dynamically some events during test and apply filters to get stats for these
    // events
    EventParticipantStats eventParticipantStats =
        managerApi.getEventParticipantStats(STUDENT1_ID, null, null);
    assertNotEquals(0, eventParticipantStats.getTotalEvents());
  }
}
