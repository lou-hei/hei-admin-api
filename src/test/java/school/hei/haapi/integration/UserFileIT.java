package school.hei.haapi.integration;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.ENABLED;
import static school.hei.haapi.endpoint.rest.model.FileType.TRANSCRIPT;
import static school.hei.haapi.integration.SchoolFileIT.setUpRestTemplate;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE4_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.ORGANIZER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.PAYMENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT11_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT12_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT13_TOKEN;
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
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.FilesApi;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Coordinates;
import school.hei.haapi.endpoint.rest.model.CrupdateStudent;
import school.hei.haapi.endpoint.rest.model.FileInfo;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.endpoint.rest.model.ZipReceiptsRequest;
import school.hei.haapi.endpoint.rest.model.ZipReceiptsStatistic;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.service.utils.ScholarshipCertificateDataProvider;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class UserFileIT extends FacadeITMockedThirdParties {
  @MockBean private EventBridgeClient eventBridgeClientMock;
  @MockBean RestTemplate restTemplateMock;
  @MockBean private ScholarshipCertificateDataProvider scholarshipCertificateDataProvider;

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, student1());
    setUpRestTemplate(restTemplateMock);
    setUpScholarshipCertificateDataProvider(scholarshipCertificateDataProvider);
  }

  private void setUpScholarshipCertificateDataProvider(
      ScholarshipCertificateDataProvider provider) {
    when(provider.getAcademicYearSentence(any())).thenReturn("test academic year");
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
  void student_load_certificate_via_http_client_ok()
      throws IOException, InterruptedException, ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    UsersApi usersApi = new UsersApi(managerClient);

    CrupdateStudent toSave =
        new CrupdateStudent()
            .id("student12_id")
            .ref("STD25012")
            .firstName("Student")
            .lastName("Eleven")
            .address("Addr 6")
            .nic("0000000000000")
            .status(ENABLED)
            .email("test+student12@hei.school")
            .coordinates(new Coordinates().latitude(20.2325d).longitude(24.5625d))
            .highSchoolOrigin("lycée analamahitsy")
            .entranceDatetime(Instant.parse("2021-11-09T08:26:24.00Z"))
            .birthDate(LocalDate.now().minusYears(19));

    Student underagedStudent = usersApi.createOrUpdateStudents(List.of(toSave), null).getFirst();

    String STUDENT_CERTIFICATE =
        "/students/" + underagedStudent.getId() + "/scholarship_certificate/raw";
    HttpClient httpClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + STUDENT_CERTIFICATE))
                .GET()
                .header("Authorization", "Bearer " + STUDENT12_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(response.body());
    assertNotNull(response);
  }

  @Test
  void student_underage_get_certificate_ok()
      throws IOException, InterruptedException, ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    UsersApi usersApi = new UsersApi(managerClient);

    CrupdateStudent toSave =
        new CrupdateStudent()
            .id("student11_id")
            .ref("STD25011")
            .firstName("Student")
            .lastName("Eleven")
            .address("Addr 6")
            .status(ENABLED)
            .email("test+student11@hei.school")
            .coordinates(new Coordinates().latitude(20.2325d).longitude(24.5625d))
            .highSchoolOrigin("lycée analamahitsy")
            .entranceDatetime(Instant.parse("2021-11-09T08:26:24.00Z"))
            .birthDate(LocalDate.now().minusYears(16));

    Student underagedStudent = usersApi.createOrUpdateStudents(List.of(toSave), null).getFirst();

    String STUDENT_CERTIFICATE =
        "/students/" + underagedStudent.getId() + "/scholarship_certificate/raw";
    HttpClient httpClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + STUDENT_CERTIFICATE))
                .GET()
                .header("Authorization", "Bearer " + STUDENT11_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(response.body());
    assertNotNull(response);
  }

  @Test
  void student_missing_nic_get_certificate_ko()
      throws IOException, InterruptedException, ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    UsersApi usersApi = new UsersApi(managerClient);

    CrupdateStudent toSave =
        new CrupdateStudent()
            .id("student13_id")
            .ref("STD25013")
            .firstName("Student")
            .lastName("Thirteen")
            .address("Addr 6")
            .status(ENABLED)
            .email("test+student13@hei.school")
            .coordinates(new Coordinates().latitude(20.2325d).longitude(24.5625d))
            .highSchoolOrigin("lycée analamahitsy")
            .entranceDatetime(Instant.parse("2021-11-09T08:26:24.00Z"))
            .birthDate(LocalDate.now().minusYears(18));

    Student underagedStudent = usersApi.createOrUpdateStudents(List.of(toSave), null).getFirst();

    String STUDENT_CERTIFICATE =
        "/students/" + underagedStudent.getId() + "/scholarship_certificate/raw";
    HttpClient httpClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<String> response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + STUDENT_CERTIFICATE))
                .GET()
                .header("Authorization", "Bearer " + STUDENT13_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(FORBIDDEN.value(), response.statusCode());
    assertEquals(
        "{"
            + "\"type\":\"403 FORBIDDEN\","
            + "\"message\":\"Please complete your information "
            + "at the Administration to be able to get your certificate.\"}",
        response.body());
  }

  @Test
  @Disabled("Ask if 4e and 5e years are now allowed")
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
  void organizer_load_other_files_ko() {
    ApiClient organizerClient = anApiClient(ORGANIZER1_TOKEN);
    FilesApi api = new FilesApi(organizerClient);
    assertThrowsForbiddenException(() -> api.getUserFiles(STUDENT1_ID, 1, 15, null));
  }

  @Test
  @Disabled("TODO: maybe student get disabled somewhere")
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
  @Disabled("TODO: maybe student get disabled somewhere")
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

    assertTrue(documents.contains(file1()));
  }

  @Test
  void manager_create_zip_contain_receipt_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    ZipReceiptsStatistic zipReceiptsStatistic =
        api.getZipFeeReceipts(
            new ZipReceiptsRequest()
                .destinationEmail("email")
                .from(Instant.parse("2021-11-08T08:25:24.00Z"))
                .to(Instant.now()));
    assertNotNull(zipReceiptsStatistic);
  }

  public static FileInfo file1() {
    return new FileInfo()
        .id("file1_id")
        .fileType(TRANSCRIPT)
        .name("transcript1")
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
  }
}
