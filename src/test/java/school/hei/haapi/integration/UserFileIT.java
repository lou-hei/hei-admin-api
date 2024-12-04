package school.hei.haapi.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.FileType.TRANSCRIPT;
import static school.hei.haapi.integration.SchoolFileIT.setUpRestTemplate;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.UserFileIT.ContextInitializer.SERVER_PORT;
import static school.hei.haapi.integration.conf.TestUtils.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.FilesApi;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.FileInfo;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserFileIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
public class UserFileIT extends MockedThirdParties {
  @MockBean private EventBridgeClient eventBridgeClientMock;
  @MockBean RestTemplate restTemplateMock;
  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, student1());
    setUpRestTemplate(restTemplateMock);
  }

  @Test
  void student_load_other_certificate_ko() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    assertThrowsForbiddenException(() -> api.getStudentScholarshipCertificate(STUDENT2_ID));
  }

  @Test
  void student_load_other_fee_receipt_ko() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsForbiddenException(() -> api.getPaidFeeReceipt(STUDENT2_ID, FEE4_ID, PAYMENT1_ID));
  }

  @Test
  void student_load_fee_receipt_ok() throws IOException, InterruptedException {
    String FEE_RECEIPT_RAW =
        "/students/"
            + STUDENT1_ID
            + "/fees/"
            + FEE1_ID
            + "/payments/"
            + PAYMENT1_ID
            + "/receipt/raw";
    HttpClient httpClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + SERVER_PORT;

    HttpResponse response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + FEE_RECEIPT_RAW))
                .GET()
                .header("Authorization", "Bearer " + STUDENT1_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(response.body());
    assertNotNull(response);
  }

  @Test
  void student_load_certificate_via_http_client_ok() throws IOException, InterruptedException {
    String STUDENT_CERTIFICATE = "/students/" + STUDENT1_ID + "/scholarship_certificate/raw";
    HttpClient httpClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + SERVER_PORT;

    HttpResponse response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + STUDENT_CERTIFICATE))
                .GET()
                .header("Authorization", "Bearer " + STUDENT1_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(response.body());
    assertNotNull(response);
  }

  @Test
  void monitor_load_followed_student_certificate_via_http_client_ok()
      throws IOException, InterruptedException {
    String STUDENT_CERTIFICATE = "/students/" + STUDENT1_ID + "/scholarship_certificate/raw";
    HttpClient httpClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + SERVER_PORT;

    HttpResponse response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + STUDENT_CERTIFICATE))
                .GET()
                .header("Authorization", "Bearer " + MONITOR1_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(response.body());
    assertNotNull(response);
  }

  @Test
  void student_load_other_files_ko() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    assertThrowsForbiddenException(() -> api.getUserFiles(STUDENT2_ID, 1, 15, null));
  }

  @Test
  void teacher_load_other_files_ko() throws ApiException {
    ApiClient student1Client = anApiClient(TEACHER1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    assertThrowsForbiddenException(() -> api.getUserFiles(STUDENT2_ID, 1, 15, null));
  }

  @Test
  void teacher_read_own_files_ok() throws ApiException {
    ApiClient student1Client = anApiClient(TEACHER1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    List<FileInfo> actual = api.getUserFiles(TEACHER1_ID, 1, 15, null);
    assertEquals(1, actual.size());
  }

  @Test
  void student_read_own_files_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    List<FileInfo> documents = api.getUserFiles(STUDENT1_ID, 1, 15, null);

    assertEquals(2, documents.size());
    assertTrue(documents.contains(file1()));
  }

  @Test
  void monitor_read_own_followed_student_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    FilesApi api = new FilesApi(monitor1Client);

    List<FileInfo> documents = api.getUserFiles(STUDENT1_ID, 1, 15, null);
    FileInfo document = api.getUserFilesById(STUDENT1_ID, "file1_id");

    assertEquals(2, documents.size());
    assertTrue(documents.contains(file1()));
    assertNotNull(document);
  }

  @Test
  void monitor_read_other_student_ko() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    FilesApi api = new FilesApi(monitor1Client);

    assertThrowsForbiddenException(() -> api.getUserFiles(STUDENT2_ID, 1, 15, null));
    assertThrowsForbiddenException(() -> api.getUserFilesById(STUDENT2_ID, "file_id"));
  }

  @Test
  void student_read_own_transcripts_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    List<FileInfo> documents = api.getUserFiles(STUDENT1_ID, 1, 15, TRANSCRIPT);

    assertEquals(1, documents.size());
    assertTrue(documents.contains(file1()));
  }

  @Test
  void manager_read_student_files_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    FilesApi api = new FilesApi(manager1Client);

    List<FileInfo> documents = api.getUserFiles(STUDENT1_ID, 1, 15, null);

    assertEquals(2, documents.size());
    assertTrue(documents.contains(file1()));
  }

  public static FileInfo file1() {
    return new FileInfo()
        .id("file1_id")
        .fileType(TRANSCRIPT)
        .name("transcript1")
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, UserFileIT.ContextInitializer.SERVER_PORT);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
