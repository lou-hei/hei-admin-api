package school.hei.haapi.integration;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.ENABLED;
import static school.hei.haapi.endpoint.rest.model.Sex.F;
import static school.hei.haapi.endpoint.rest.model.Sex.M;
import static school.hei.haapi.integration.conf.TestUtils.ORGANIZER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.ORGANIZER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.ORGANIZER2_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.someCreatableEvent;
import static school.hei.haapi.integration.conf.TestUtils.uploadProfilePicture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.EventsApi;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Coordinates;
import school.hei.haapi.endpoint.rest.model.CreateEvent;
import school.hei.haapi.endpoint.rest.model.Event;
import school.hei.haapi.endpoint.rest.model.EventType;
import school.hei.haapi.endpoint.rest.model.Organizer;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@Slf4j
@Testcontainers
@AutoConfigureMockMvc
public class OrganizerIT extends FacadeITMockedThirdParties {
  @Autowired ObjectMapper objectMapper;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  public static Organizer organizer1() {
    Organizer organizer = new Organizer();
    organizer.setId("organizer1_id");
    organizer.setFirstName("Organizer 1");
    organizer.setLastName("Smith");
    organizer.setEmail("test+organizer@hei.school");
    organizer.setRef("ORG22001");
    organizer.setPhone("0322400028");
    organizer.setStatus(ENABLED);
    organizer.setSex(M);
    organizer.setBirthDate(LocalDate.parse("1980-10-10"));
    organizer.setEntranceDatetime(Instant.parse("2022-09-08T08:25:29.00Z"));
    organizer.setAddress("Adr 10");
    organizer.setBirthPlace("");
    organizer.setNic("");
    organizer.setCoordinates(new Coordinates().longitude(55.555).latitude(-55.555));
    return organizer;
  }

  public static Organizer organizer2() {
    Organizer organizer = new Organizer();
    organizer.setId("organizer2_id");
    organizer.setFirstName("Organizer 2");
    organizer.setLastName("Doe");
    organizer.setEmail("test+organizer+2@hei.school");
    organizer.setRef("ORG22002");
    organizer.setPhone("0322411113");
    organizer.setStatus(ENABLED);
    organizer.setSex(F);
    organizer.setBirthDate(LocalDate.parse("1890-01-01"));
    organizer.setEntranceDatetime(Instant.parse("2022-09-08T08:25:29.00Z"));
    organizer.setAddress("Adr 12");
    organizer.setBirthPlace("");
    organizer.setNic("");
    organizer.setCoordinates(new Coordinates().longitude(55.555).latitude(-55.555));
    return organizer;
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
  }

  @Test
  @Disabled("Not implemented: organizer can modify picture")
  void organizer_update_own_profile_picture() throws IOException, InterruptedException {
    HttpResponse<InputStream> response =
        uploadProfilePicture(localPort, ORGANIZER1_TOKEN, ORGANIZER1_ID, "organizer");

    Organizer organizer = objectMapper.readValue(response.body(), Organizer.class);

    assertEquals(200, response.statusCode());
    assertEquals(organizer1().getRef(), organizer.getRef());
  }

  @Test
  void read_student_ko() {
    ApiClient student1Client = anApiClient(ORGANIZER2_TOKEN);
    UsersApi api = new UsersApi(student1Client);
    assertThrowsForbiddenException(() -> api.getStudentById(STUDENT1_ID));
    assertThrowsForbiddenException(
        () -> api.getStudents(1, 20, null, null, null, null, null, null, null, null, null));
  }

  @Test
  void read_teacher_ko() {
    ApiClient teacher1Client = anApiClient(ORGANIZER1_TOKEN);
    UsersApi api = new UsersApi(teacher1Client);
    assertThrowsForbiddenException(() -> api.getTeacherById(TEACHER1_ID));
    assertThrowsForbiddenException(() -> api.getTeachers(1, 20, null, null, null, null, null));
  }

  @Test
  void read_events_ok() throws ApiException {
    ApiClient organizerClient = anApiClient(ORGANIZER1_TOKEN);
    EventsApi api = new EventsApi(organizerClient);
    List<Event> events = api.getEvents(1, 10, null, null, null, null, null);

    assertFalse(events.isEmpty());
  }

  @Test
  void manipulate_events_ok() throws ApiException {
    ApiClient organizerClient = anApiClient(ORGANIZER1_TOKEN);
    EventsApi api = new EventsApi(organizerClient);

    CreateEvent createEvent =
        someCreatableEvent(
            EventType.EXAM, ORGANIZER1_ID, Instant.now(), Instant.now().plus(1, HOURS));

    List<Event> events = api.crupdateEvents(List.of(createEvent), null, null, null, null);
    Event newEvent = events.getFirst();
    assertEquals(createEvent.getTitle(), newEvent.getTitle());

    api.deleteEventById(newEvent.getId());
    assertThrows(ApiException.class, () -> api.getEventById(newEvent.getId()));
  }
}
