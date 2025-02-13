package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.Scope.TEACHER;
import static school.hei.haapi.integration.ManagerIT.manager1;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.ANNOUNCEMENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.ANNOUNCEMENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.ANNOUNCEMENT3_ID;
import static school.hei.haapi.integration.conf.TestUtils.ANNOUNCEMENT4_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.announcementEspeciallyForG1;
import static school.hei.haapi.integration.conf.TestUtils.announcementForAll;
import static school.hei.haapi.integration.conf.TestUtils.announcementForManager;
import static school.hei.haapi.integration.conf.TestUtils.announcementForTeacher;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.createAnnouncementWithGroupTarget;
import static school.hei.haapi.integration.conf.TestUtils.expectedAnnouncementCreated1;
import static school.hei.haapi.integration.conf.TestUtils.setUpCasdoor;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.rest.api.AnnouncementsApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Announcement;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@Testcontainers
@AutoConfigureMockMvc
public class AnnouncementIT extends FacadeITMockedThirdParties {

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @MockBean EventProducer producer;

  @Test
  void manager_read_announcements_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    // Get all announcements
    List<Announcement> allAnnouncement = api.getAnnouncements(1, 15, null, null, null, null);
    assertTrue(
        allAnnouncement.containsAll(
            List.of(
                announcementForAll(),
                announcementEspeciallyForG1(),
                announcementForManager(),
                announcementForTeacher())));

    // Get all announcements filtered by author ref
    List<Announcement> announcementsFilteredByAuthorRef =
        api.getAnnouncements(1, 15, null, null, manager1().getRef(), null);
    assertTrue(announcementsFilteredByAuthorRef.contains(announcementForAll()));
    assertTrue(announcementsFilteredByAuthorRef.contains(announcementForTeacher()));
    assertFalse(announcementsFilteredByAuthorRef.contains(announcementEspeciallyForG1()));

    // Get all announcements filtered by date
    List<Announcement> announcementsFilteredByCreationDatetime =
        api.getAnnouncements(
            1,
            15,
            Instant.parse("2022-12-21T08:00:00.00Z"),
            Instant.parse("2022-12-22T08:00:00.00Z"),
            null,
            null);
    assertFalse(announcementsFilteredByCreationDatetime.contains(announcementForAll()));
    assertTrue(announcementsFilteredByCreationDatetime.contains(announcementForTeacher()));
    assertTrue(announcementsFilteredByCreationDatetime.contains(announcementEspeciallyForG1()));

    // Get all announcements filtered by scope
    List<Announcement> announcementsFilteredByScope =
        api.getAnnouncements(1, 15, null, null, null, TEACHER);
    assertTrue(announcementsFilteredByScope.contains(announcementForTeacher()));
    assertFalse(announcementsFilteredByScope.contains(announcementEspeciallyForG1()));
    assertFalse(announcementsFilteredByScope.contains(announcementForManager()));
  }

  @Test
  void manager_read_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    Announcement actual = api.getAnnouncementById(ANNOUNCEMENT1_ID);
    assertEquals(announcementForAll(), actual);
  }

  @Test
  void teacher_read_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    Announcement actual = api.getTeacherAnnouncementById(ANNOUNCEMENT2_ID);
    assertEquals(announcementForTeacher(), actual);
  }

  @Test
  void student_read_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    Announcement actual = api.getStudentsAnnouncementById(ANNOUNCEMENT3_ID);
    assertEquals(announcementEspeciallyForG1(), actual);
  }

  @Test
  void monitor_read_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MONITOR1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    Announcement actual = api.getStudentsAnnouncementById(ANNOUNCEMENT3_ID);
    assertEquals(announcementEspeciallyForG1(), actual);
  }

  @Test
  void read_by_id_ko() {
    ApiClient studentApiClient = anApiClient(STUDENT1_TOKEN);
    AnnouncementsApi apiStudent = new AnnouncementsApi(studentApiClient);

    ApiClient teacherApiClient = anApiClient(TEACHER1_TOKEN);
    AnnouncementsApi apiTeacher = new AnnouncementsApi(teacherApiClient);

    ApiClient monitorApiClient = anApiClient(MONITOR1_TOKEN);
    AnnouncementsApi apiMonitor = new AnnouncementsApi(monitorApiClient);

    assertThrowsForbiddenException(() -> apiStudent.getTeacherAnnouncementById(ANNOUNCEMENT2_ID));
    assertThrowsForbiddenException(() -> apiTeacher.getAnnouncementById(ANNOUNCEMENT4_ID));
    assertThrowsForbiddenException(() -> apiMonitor.getTeacherAnnouncementById(ANNOUNCEMENT2_ID));
  }

  @Test
  void manager_create_announcement_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    Announcement announcementWithTargetCreated =
        api.createAnnouncement(createAnnouncementWithGroupTarget());
    assertEquals(
        expectedAnnouncementCreated1().getScope(), createAnnouncementWithGroupTarget().getScope());
    assertEquals(
        expectedAnnouncementCreated1().getAuthor(), announcementWithTargetCreated.getAuthor());
    assertEquals(
        expectedAnnouncementCreated1().getTitle(), announcementWithTargetCreated.getTitle());
  }

  @Test
  void student_read_only_announcement_for_student_ok() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    List<Announcement> actual = api.getStudentsAnnouncements(1, 15, null, null, null, null);
    assertTrue(actual.contains(announcementForAll()));
    assertTrue(actual.contains(announcementEspeciallyForG1()));
    assertFalse(actual.contains(announcementForTeacher()));
  }

  @Test
  void monitor_read_only_announcement_for_student_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MONITOR1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    List<Announcement> actual = api.getStudentsAnnouncements(1, 15, null, null, null, null);
    assertTrue(actual.contains(announcementForAll()));
    assertTrue(actual.contains(announcementEspeciallyForG1()));
    assertFalse(actual.contains(announcementForTeacher()));
  }

  @Test
  void student_read_all_announcement_or_for_teacher_ko() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    assertThrowsForbiddenException(() -> api.getAnnouncements(1, 15, null, null, null, null));
    assertThrowsForbiddenException(() -> api.getTeachersAnnouncements(1, 15, null, null, null));
  }

  @Test
  void monitor_read_all_announcement_or_for_teacher_ko() throws ApiException {
    ApiClient apiClient = anApiClient(MONITOR1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    assertThrowsForbiddenException(() -> api.getAnnouncements(1, 15, null, null, null, null));
    assertThrowsForbiddenException(() -> api.getTeachersAnnouncements(1, 15, null, null, null));
  }

  @Test
  void teacher_read_announcements_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    AnnouncementsApi api = new AnnouncementsApi(apiClient);

    List<Announcement> actual = api.getTeachersAnnouncements(1, 15, null, null, null);
    assertTrue(
        actual.containsAll(
            List.of(
                announcementForAll(), announcementForTeacher(), announcementEspeciallyForG1())));
    assertFalse(actual.contains(announcementForManager()));
  }

  @BeforeEach
  void setUp() {
    setUpCasdoor(casdoorAuthServiceMock);
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }
}
