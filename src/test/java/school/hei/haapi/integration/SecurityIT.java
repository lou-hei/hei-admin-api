package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.Whoami.RoleEnum.MONITOR;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.SUSPENDED_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.SecurityApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Whoami;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = SecurityIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
class SecurityIT extends MockedThirdParties {
  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  public static Whoami whoisStudent1() {
    Whoami whoami = new Whoami();
    whoami.setId("student1_id");
    whoami.setBearer(STUDENT1_TOKEN);
    whoami.setRole(Whoami.RoleEnum.STUDENT);
    return whoami;
  }

  public static Whoami whoisMonitor1() {
    Whoami whoami = new Whoami();
    whoami.setId("monitor1_id");
    whoami.setBearer(MONITOR1_TOKEN);
    whoami.setRole(MONITOR);
    return whoami;
  }

  public static Whoami whoisTeacher1() {
    Whoami whoami = new Whoami();
    whoami.setId("teacher1_id");
    whoami.setBearer(TEACHER1_TOKEN);
    whoami.setRole(Whoami.RoleEnum.TEACHER);
    return whoami;
  }

  public static Whoami whoisManager1() {
    Whoami whoami = new Whoami();
    whoami.setId("manager1_id");
    whoami.setBearer(MANAGER1_TOKEN);
    whoami.setRole(Whoami.RoleEnum.MANAGER);
    return whoami;
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void monitor_read_whoami_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);

    SecurityApi api = new SecurityApi(monitor1Client);
    Whoami actual = api.whoami();

    assertEquals(whoisMonitor1(), actual);
  }

  @Test
  void student_read_whoami_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);

    SecurityApi api = new SecurityApi(student1Client);
    Whoami actual = api.whoami();

    assertEquals(whoisStudent1(), actual);
  }

  @Test
  void teacher_read_whoami_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

    SecurityApi api = new SecurityApi(teacher1Client);
    Whoami actual = api.whoami();

    assertEquals(whoisTeacher1(), actual);
  }

  @Test
  void manager_read_whoami_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);

    SecurityApi api = new SecurityApi(manager1Client);
    Whoami actual = api.whoami();

    assertEquals(whoisManager1(), actual);
  }

  @Test
  void manager_read_unknown_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;

    HttpResponse<String> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/unknown"))
                .header("Authorization", "Bearer " + MANAGER1_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.FORBIDDEN.value(), response.statusCode());
    assertEquals(
        "{" + "\"type\":\"403 FORBIDDEN\"," + "\"message\":\"Access is denied\"}", response.body());
  }

  @Test
  void non_authorized_path_for_suspended_user_ko() {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;
    List<String> nonAuthorizedPaths =
        List.of(
            "/school/files",
            "/school/files/*",
            "/school/files",
            "/school/files/*",
            "/students/*/work_files",
            "/students/*/work_files/*",
            "/students/*/files",
            "/students/*/files/*",
            "/students/*/scholarship_certificate/raw",
            "/announcements",
            "/announcements/*");

    nonAuthorizedPaths.forEach(
        path -> {
          HttpResponse<String> response = null;
          try {
            response =
                unauthenticatedClient.send(
                    HttpRequest.newBuilder()
                        .uri(URI.create(basePath + path))
                        .header("Authorization", "Bearer " + SUSPENDED_TOKEN)
                        .build(),
                    HttpResponse.BodyHandlers.ofString());
          } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
          }

          assertEquals(HttpStatus.FORBIDDEN.value(), response.statusCode());
          assertEquals(
              "{" + "\"type\":\"403 FORBIDDEN\"," + "\"message\":\"Access is denied\"}",
              response.body());
        });
  }

  @Test
  void suspended_read_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;

    HttpResponse<String> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/non-accessible-by-suspended"))
                .header("Authorization", "Bearer " + SUSPENDED_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.FORBIDDEN.value(), response.statusCode());
    assertEquals(
        "{" + "\"type\":\"403 FORBIDDEN\"," + "\"message\":\"Access is denied\"}", response.body());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
