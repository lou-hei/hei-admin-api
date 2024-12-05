package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static school.hei.haapi.endpoint.rest.model.FileType.DOCUMENT;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.FilesApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.FileInfo;
import school.hei.haapi.endpoint.rest.model.ShareInfo;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
public class SchoolFileIT extends FacadeITMockedThirdParties {
  @MockBean EventBridgeClient eventBridgeClientMock;
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

  public static void setUpRestTemplate(RestTemplate restTemplateMock) {
    when(restTemplateMock.exchange(any(), eq(POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(OCS_MOCKED_RESPONSE));
  }

  private static final String OCS_MOCKED_RESPONSE =
      """
          {
            "ocs": {
              "meta": {
                "status": "ok",
                "statuscode": 100,
                "message": null,
                "totalitems": "",
                "itemsperpage": ""
              },
              "data": {
                "id": "130",
                "share_type": 3,
                "uid_owner": "ilo",
                "displayname_owner": "john",
                "permissions": 15,
                "stime": 1719915415,
                "parent": null,
                "expiration": "2024-07-03 00:00:00",
                "token": "vDq5Er8qizxQOEB",
                "uid_file_owner": "john",
                "displayname_file_owner": "john",
                "additional_info_owner": null,
                "additional_info_file_owner": null,
                "path": "/Test-api",
                "mimetype": "httpd/unix-directory",
                "storage_id": "object::user:john",
                "storage": 66,
                "item_type": "folder",
                "item_source": 22602,
                "file_source": 22602,
                "file_parent": 22570,
                "file_target": "/Test-api",
                "name": "test",
                "url": "https://owncloud.example.com/s/vDq5Er8qizxQOEB",
                "mail_send": 0,
                "attributes": null
              }
            }
          }""";

  @Test
  void manager_get_share_link() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    FilesApi filesApi = new FilesApi(apiClient);

    ShareInfo actual = filesApi.getSchoolFilesShareLink("/Test-api");
    assertTrue(actual.getPath().contains("/Test-api"));
    assertTrue(actual.getUrl().contains("https://owncloud.example.com"));
  }

  @Test
  @Disabled("TODO")
  void student_get_share_link() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    FilesApi filesApi = new FilesApi(apiClient);

    ShareInfo actual = filesApi.getSchoolFilesShareLink("/Test-api");
    assertTrue(actual.getPath().contains("/Test-api"));
    assertTrue(actual.getUrl().contains("https://owncloud.example.com"));
  }

  @Test
  void teacher_get_share_link() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    FilesApi filesApi = new FilesApi(apiClient);

    ShareInfo actual = filesApi.getSchoolFilesShareLink("/Test-api");
    assertTrue(actual.getPath().contains("/Test-api"));
    assertTrue(actual.getUrl().contains("https://owncloud.example.com"));
  }

  @Test
  @Disabled("TODO")
  void student_read_school_files_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    List<FileInfo> schoolRegulations = api.getSchoolRegulations(1, 15);

    assertTrue(schoolRegulations.contains(schoolFile()));
    assertEquals(1, schoolRegulations.size());
  }

  @Test
  void monitor_read_school_files_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    FilesApi api = new FilesApi(monitor1Client);

    List<FileInfo> schoolRegulations = api.getSchoolRegulations(1, 15);

    assertTrue(schoolRegulations.contains(schoolFile()));
    assertEquals(1, schoolRegulations.size());
  }

  @Test
  void teacher_read_school_files_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    FilesApi api = new FilesApi(teacher1Client);

    List<FileInfo> schoolRegulations = api.getSchoolRegulations(1, 15);

    assertTrue(schoolRegulations.contains(schoolFile()));
    assertEquals(1, schoolRegulations.size());
  }

  @Test
  void manager_read_school_files_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    FilesApi api = new FilesApi(manager1Client);

    List<FileInfo> schoolRegulations = api.getSchoolRegulations(1, 15);

    assertTrue(schoolRegulations.contains(schoolFile()));
    assertEquals(1, schoolRegulations.size());
  }

  @Test
  void manager_read_school_file_by_id_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    FilesApi api = new FilesApi(manager1Client);

    FileInfo schoolRegulation = api.getSchoolRegulationById("file3_id");

    assertEquals(schoolFile(), schoolRegulation);
  }

  public static FileInfo schoolFile() {
    return new FileInfo()
        .id("file3_id")
        .fileType(DOCUMENT)
        .name("school_file")
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
  }
}
