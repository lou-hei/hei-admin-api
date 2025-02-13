package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static school.hei.haapi.endpoint.rest.model.FileType.WORK_DOCUMENT;
import static school.hei.haapi.endpoint.rest.model.ProfessionalExperienceFileTypeEnum.BUSINESS_OWNER;
import static school.hei.haapi.endpoint.rest.model.ProfessionalExperienceFileTypeEnum.WORKER_STUDENT;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.getMockedFile;
import static school.hei.haapi.integration.conf.TestUtils.setUpCasdoor;
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
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.FilesApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.WorkDocumentInfo;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
public class WorkDocumentIT extends FacadeITMockedThirdParties {
  @MockBean private EventBridgeClient eventBridgeClientMock;
  public static String WORK_DOCUMENT_1_ID = "work_file1_id";

  @BeforeEach
  public void setUp() {
    setUpCasdoor(casdoorAuthServiceMock);
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, student1());
  }

  public static WorkDocumentInfo workDocument1() {
    return new WorkDocumentInfo()
        .id("work_file1_id")
        .name("work file")
        .fileType(WORK_DOCUMENT)
        .professionalExperience(WORKER_STUDENT)
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
  }

  public static WorkDocumentInfo workDocumentInfoBusinessOwnerStudent1() {
    return new WorkDocumentInfo()
        .professionalExperience(BUSINESS_OWNER)
        .id("work_file3_id")
        .fileType(WORK_DOCUMENT)
        .name("business file")
        .creationDatetime(Instant.parse("2020-11-08T08:25:24.00Z"));
  }

  @Test
  void manager_create_student_work_documents_with_bad_field_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    FilesApi api = new FilesApi(manager1Client);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Commitment begin must be less than commitment"
            + " end\"}",
        () -> {
          api.uploadStudentWorkFile(
              STUDENT1_ID,
              "test",
              Instant.parse("2021-11-09T08:25:24.00Z"),
              BUSINESS_OWNER,
              Instant.parse("2021-11-08T08:25:24.00Z"),
              Instant.parse("2021-11-08T08:25:24.00Z"),
              getMockedFile("img", ".png"));
        });
  }

  @Test
  void manager_read_student_work_documents_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    FilesApi api = new FilesApi(manager1Client);

    List<WorkDocumentInfo> workDocuments = api.getStudentWorkDocuments(STUDENT1_ID, 1, 10, null);

    assertEquals(3, workDocuments.size());
    assertEquals(workDocument1(), workDocuments.get(0));
  }

  @Test
  @Disabled("TODO: maybe student get disabled somewhere")
  void student_read_own_work_document_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    FilesApi api = new FilesApi(student1Client);

    List<WorkDocumentInfo> workDocuments = api.getStudentWorkDocuments(STUDENT1_ID, 1, 10, null);

    assertEquals(3, workDocuments.size());
    assertEquals(workDocument1(), workDocuments.get(0));
  }

  @Test
  void monitor_read_own_student_followed_work_document_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    FilesApi api = new FilesApi(monitor1Client);

    List<WorkDocumentInfo> workDocuments = api.getStudentWorkDocuments(STUDENT1_ID, 1, 10, null);

    assertEquals(3, workDocuments.size());
    assertEquals(workDocument1(), workDocuments.get(0));
  }

  @Test
  void manager_read_work_documents_by_professional_type_and_student_id() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    FilesApi api = new FilesApi(manager1Client);

    List<WorkDocumentInfo> workDocuments =
        api.getStudentWorkDocuments(STUDENT1_ID, 1, 10, BUSINESS_OWNER);

    assertEquals(1, workDocuments.size());
    assertEquals(workDocumentInfoBusinessOwnerStudent1(), workDocuments.get(0));
  }

  @Test
  void manager_read_work_documents_by_id_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    FilesApi api = new FilesApi(manager1Client);

    WorkDocumentInfo actual = api.getStudentWorkDocumentsById(STUDENT1_ID, WORK_DOCUMENT_1_ID);

    assertEquals(workDocument1(), actual);
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }
}
