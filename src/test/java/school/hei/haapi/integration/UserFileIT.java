package school.hei.haapi.integration;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.FileType.TRANSCRIPT;
import static school.hei.haapi.integration.SchoolFileIT.setUpRestTemplate;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE4_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.PAYMENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.FilesApi;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.FileInfo;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
public class UserFileIT extends FacadeITMockedThirdParties {
  @MockBean private EventBridgeClient eventBridgeClientMock;
  @MockBean RestTemplate restTemplateMock;

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, student1());
    setUpRestTemplate(restTemplateMock);
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @Test
  void student_load_other_certificate_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    assertThrowsForbiddenException(() -> api.getStudentScholarshipCertificate(STUDENT2_ID));
  }

  @Test
  void student_load_other_fee_receipt_ko() {
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
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
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
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
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
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
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
  void student_load_other_files_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    assertThrowsForbiddenException(() -> api.getUserFiles(STUDENT2_ID, 1, 15, null));
  }

  @Test
  void teacher_load_other_files_ko() {
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
  void monitor_read_other_student_ko() {
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
}
