package school.hei.haapi.integration;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER2_ID;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.coordinatesWithNullValues;
import static school.hei.haapi.integration.conf.TestUtils.coordinatesWithValues;
import static school.hei.haapi.integration.conf.TestUtils.isValidUUID;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.integration.conf.TestUtils.someCreatableTeacher;
import static school.hei.haapi.integration.conf.TestUtils.someCreatableTeacherList;
import static school.hei.haapi.integration.conf.TestUtils.teacher1;
import static school.hei.haapi.integration.conf.TestUtils.teacher2;
import static school.hei.haapi.integration.conf.TestUtils.uploadProfilePicture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.mapper.UserMapper;
import school.hei.haapi.endpoint.rest.model.CrupdateTeacher;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.Sex;
import school.hei.haapi.endpoint.rest.model.Teacher;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.service.UserService;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TeacherIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
class TeacherIT extends MockedThirdParties {
  @MockBean private EventBridgeClient eventBridgeClientMock;

  @Autowired private UserService userService;

  @Autowired private UserMapper userMapper;

  @Autowired private ObjectMapper objectMapper;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, teacher1());
  }

  @Test
  void teacher_update_own_profile_picture() throws IOException, InterruptedException {
    HttpResponse<InputStream> response =
        uploadProfilePicture(
            TeacherIT.ContextInitializer.SERVER_PORT, TEACHER1_TOKEN, TEACHER1_ID, "teachers");

    Teacher teacher = objectMapper.readValue(response.body(), Teacher.class);

    assertEquals(200, response.statusCode());
    assertEquals("TCR21001", teacher.getRef());
  }

  @Test
  @DirtiesContext
  void teacher_update_own_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(teacher1Client);
    UsersApi managerApi = new UsersApi(manager1Client);
    api.getStudentById(STUDENT1_ID);
    Teacher actual = api.updateTeacher(TEACHER1_ID, someUpdatableTeacher1());
    List<Teacher> actualTeachers = managerApi.getTeachers(1, 10, null, null, null, null, null);

    assertTrue(actualTeachers.contains(actual));
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);

    UsersApi api = new UsersApi(student1Client);
    assertThrowsForbiddenException(() -> api.getTeacherById(TEACHER1_ID));
    assertThrowsForbiddenException(() -> api.getTeachers(1, 20, null, null, null, null, null));
  }

  @Test
  void teacher_read_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

    UsersApi api = new UsersApi(teacher1Client);
    assertThrowsForbiddenException(() -> api.getTeacherById(TEACHER2_ID));
    assertThrowsForbiddenException(() -> api.getTeachers(1, 20, null, null, null, null, null));
  }

  @Test
  void student_write_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);

    UsersApi api = new UsersApi(student1Client);
    assertThrowsForbiddenException(() -> api.createOrUpdateTeachers(List.of()));
  }

  @Test
  void teacher_write_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

    UsersApi api = new UsersApi(teacher1Client);
    assertThrowsForbiddenException(() -> api.createOrUpdateTeachers(List.of()));
  }

  @Test
  void teacher_read_own_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

    UsersApi api = new UsersApi(teacher1Client);
    Teacher actual = api.getTeacherById(TEACHER1_ID);

    assertEquals(teacher1(), actual);
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);

    UsersApi api = new UsersApi(manager1Client);
    List<Teacher> teachers = api.getTeachers(1, 20, null, null, null, null, null);

    assertTrue(teachers.contains(teacher1()));
    assertTrue(teachers.contains(teacher2()));
  }

  @Test
  void manager_write_update_rollback_on_event_error() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);
    CrupdateTeacher toCreate = someCreatableTeacher();
    reset(eventBridgeClientMock);
    when(eventBridgeClientMock.putEvents((PutEventsRequest) any()))
        .thenThrow(RuntimeException.class);

    assertThrowsApiException(
        "{\"type\":\"500 INTERNAL_SERVER_ERROR\",\"message\":null}",
        () -> api.createOrUpdateTeachers(List.of(toCreate)));

    List<Teacher> actual = api.getTeachers(1, 100, null, null, null, null, null);
    assertFalse(actual.stream().anyMatch(s -> Objects.equals(toCreate.getEmail(), s.getEmail())));
  }

  @Test
  void manager_write_create_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    CrupdateTeacher toCreate = someCreatableTeacher();
    Teacher expected = expectedCreatedTeacher();

    UsersApi api = new UsersApi(manager1Client);
    List<Teacher> created = api.createOrUpdateTeachers(List.of(toCreate));

    assertEquals(1, created.size());
    Teacher created0 = created.get(0);
    assertTrue(isValidUUID(created0.getId()));
    expected.setId(created0.getId());
    expected.setRef(toCreate.getRef());
    expected.setEmail(toCreate.getEmail());
    assertEquals(expected, created0);
  }

  @Test
  @Disabled("TODO: poja has removed max put entries check")
  void manager_write_update_more_than_10_teachers_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);
    CrupdateTeacher teacherToCreate = someCreatableTeacher();
    List<CrupdateTeacher> listToCreate = someCreatableTeacherList(11);
    listToCreate.add(teacherToCreate);

    assertThrowsApiException(
        "{\"type\":\"500 INTERNAL_SERVER_ERROR\",\"message\":\"Request entries must be <= 10\"}",
        () -> api.createOrUpdateTeachers(listToCreate));

    List<Teacher> actual = api.getTeachers(1, 20, null, null, null, null, null);
    assertFalse(
        actual.stream().anyMatch(s -> Objects.equals(teacherToCreate.getEmail(), s.getEmail())));
  }

  @Test
  void manager_write_update_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);
    CrupdateTeacher toUpdate = someCreatableTeacher();

    List<Teacher> created = api.createOrUpdateTeachers(List.of(toUpdate));
    toUpdate.setId(created.get(0).getId());

    Teacher expected = expectedCreatedTeacher();
    expected.setId(created.get(0).getId());
    expected.setLastName("New last name");
    expected.setEmail(toUpdate.getEmail());
    expected.setRef(toUpdate.getRef());

    toUpdate.setLastName("New last name");

    List<Teacher> updated = api.createOrUpdateTeachers(List.of(toUpdate));

    assertEquals(1, updated.size());
    assertEquals(expected, updated.get(0));
  }

  @Test
  void manager_write_update_with_some_bad_fields_ko() {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);
    CrupdateTeacher toCreate1 =
        someCreatableTeacher()
            .firstName(null)
            .lastName(null)
            .email(null)
            .address(null)
            .phone(null)
            .ref(null);
    CrupdateTeacher toCreate2 = someCreatableTeacher().email("bademail");

    ApiException exception1 =
        assertThrows(ApiException.class, () -> api.createOrUpdateTeachers(List.of(toCreate1)));
    ApiException exception2 =
        assertThrows(ApiException.class, () -> api.createOrUpdateTeachers(List.of(toCreate2)));

    String exceptionMessage1 = exception1.getMessage();
    String exceptionMessage2 = exception2.getMessage();
    assertTrue(exceptionMessage2.contains("Email must be valid"));
    assertTrue(exceptionMessage1.contains("Last name is mandatory"));
    assertTrue(exceptionMessage1.contains("Email is mandatory"));
    assertTrue(exceptionMessage1.contains("Reference is mandatory"));
  }

  @Test
  void manager_read_by_disabled_status_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);

    List<Teacher> actualTeachers =
        api.getTeachers(1, 10, null, null, null, EnableStatus.DISABLED, null);
    assertEquals(2, actualTeachers.size());
    assertTrue(actualTeachers.contains(disabledTeacher1()));
  }

  @Test
  void manager_read_by_suspended_status_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);

    List<Teacher> actualTeachers =
        api.getTeachers(1, 10, null, null, null, EnableStatus.SUSPENDED, Sex.F);
    assertEquals(1, actualTeachers.size());
    assertEquals(actualTeachers.get(0), suspendedTeacher1());
    assertTrue(actualTeachers.contains((suspendedTeacher1())));
  }

  @Test
  void manager_read_by_status_and_sex_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);

    List<Teacher> actualTeachers =
        api.getTeachers(1, 10, null, null, null, EnableStatus.DISABLED, Sex.F);
    assertEquals(1, actualTeachers.size());
  }

  private static Teacher expectedCreatedTeacher() {
    return new Teacher()
        .firstName("Some")
        .lastName("User")
        .email(randomUUID() + "@hei.school")
        .ref("TCR21-" + randomUUID())
        .phone("0332511129")
        .status(EnableStatus.ENABLED)
        .sex(Sex.M)
        .birthDate(LocalDate.parse("2000-01-01"))
        .entranceDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .address("Adr X")
        .coordinates(coordinatesWithNullValues());
  }

  public static Teacher disabledTeacher1() {
    return new Teacher()
        .id("teacher5_id")
        .firstName("Disable")
        .lastName("One")
        .email("teacher+disable1@hei.school")
        .ref("TCR29001")
        .status(EnableStatus.DISABLED)
        .sex(Sex.M)
        .birthDate(LocalDate.parse("2000-12-01"))
        .entranceDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .phone("0322411123")
        .nic("")
        .birthPlace("")
        .address("Adr 1")
        .coordinates(coordinatesWithNullValues());
  }

  public static Teacher suspendedTeacher1() {
    return new Teacher()
        .id("teacher7_id")
        .firstName("Suspended")
        .lastName("One")
        .email("teacher+suspended@hei.school")
        .ref("TCR29003")
        .status(EnableStatus.SUSPENDED)
        .sex(Sex.F)
        .birthDate(LocalDate.parse("2000-12-02"))
        .entranceDatetime(Instant.parse("2021-11-09T08:26:24.00Z"))
        .phone("0322411124")
        .nic("")
        .birthPlace("")
        .address("Adr 2")
        .coordinates(coordinatesWithNullValues());
  }

  public static CrupdateTeacher someUpdatableTeacher1() {
    return new CrupdateTeacher()
        .id("teacher1_id")
        .email("test+teacher1@hei.school")
        .ref("TCR21001")
        .phone("0322411125")
        .status(EnableStatus.ENABLED)
        .sex(Sex.F)
        .entranceDatetime(Instant.parse("2021-10-08T08:27:24.00Z"))
        .nic("")
        .birthPlace("")
        .address("Adr 999")
        .sex(Sex.F)
        .lastName("Other last")
        .firstName("Other first")
        .birthDate(LocalDate.parse("2000-01-03"))
        .coordinates(coordinatesWithValues());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
