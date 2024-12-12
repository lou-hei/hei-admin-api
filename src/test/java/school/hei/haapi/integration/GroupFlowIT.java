package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.CreateGroupFlow.MoveTypeEnum.JOIN;
import static school.hei.haapi.endpoint.rest.model.CreateGroupFlow.MoveTypeEnum.LEAVE;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.StudentIT.student2;
import static school.hei.haapi.integration.conf.TestUtils.GROUP1_ID;
import static school.hei.haapi.integration.conf.TestUtils.GROUP2_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.group1;
import static school.hei.haapi.integration.conf.TestUtils.group2;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.TeachingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CreateGroupFlow;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
public class GroupFlowIT extends FacadeITMockedThirdParties {
  @MockBean private EventBridgeClient eventBridgeClientMock;

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, student1());
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @Test
  void manager_read_grouped_students_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);

    List<Student> group1Students = api.getStudentsByGroupId(GROUP1_ID, 1, 10, null);

    assertFalse(group1Students.isEmpty());
  }

  @Test
  void student_leaves_same_group_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);
    String expectedBody =
        "{"
            + "\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"Student has already left this group\"}";

    List<Student> actualStudentsGroup = api.getStudentsByGroupId(GROUP1_ID, 1, 20, null);

    // ... before handling duplicated leaves for this group
    assertThrowsApiException(
        expectedBody,
        () -> {
          api.moveOrDeleteStudentInGroup(STUDENT1_ID, List.of(createStudent1LeavesGroup1()));
          api.moveOrDeleteStudentInGroup(STUDENT1_ID, List.of(createStudent1LeavesGroup1()));
        });
  }

  @Test
  void insert_two_student_in_same_group_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);
    String expectedBody =
        "{" + "\"type\":\"400 BAD_REQUEST\"," + "\"message\":\"Student is already in group\"}";

    List<Student> actualStudentsGroup = api.getStudentsByGroupId(GROUP2_ID, 1, 20, null);

    // Assert that specified student is not in actual group ...
    assertFalse(actualStudentsGroup.contains(student2()));
    // ... before handling duplicated joins for this group
    assertThrowsApiException(
        expectedBody,
        () -> {
          api.moveOrDeleteStudentInGroup(STUDENT2_ID, List.of(createStudent2JoinsGroup2()));
          api.moveOrDeleteStudentInGroup(STUDENT2_ID, List.of(createStudent2JoinsGroup2()));
        });
  }

  @Test
  void manager_moves_student2_to_group2_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);

    api.moveOrDeleteStudentInGroup(
        STUDENT2_ID, List.of(createStudent2LeavesGroup1(), createStudent2JoinsGroup2()));
    List<Student> group1Students = api.getStudentsByGroupId(GROUP1_ID, 1, 10, null);
    List<Student> group2Students = api.getStudentsByGroupId(GROUP2_ID, 1, 10, null);

    Student student2moved = student2().groups(List.of(group2().size(2)));
    assertEquals(1, group1Students.size());
    assertEquals(2, group2Students.size());
    assertTrue(
        group2Students.containsAll(
            List.of(
                student2moved, student1().groups(List.of(group1().size(1), group2().size(2))))));
  }

  public CreateGroupFlow createStudent1LeavesGroup1() {
    return new CreateGroupFlow().groupId(GROUP1_ID).studentId(STUDENT1_ID).moveType(LEAVE);
  }

  public CreateGroupFlow createStudent2JoinsGroup2() {
    return new CreateGroupFlow().groupId(GROUP2_ID).studentId(STUDENT2_ID).moveType(JOIN);
  }

  public CreateGroupFlow createStudent2LeavesGroup1() {
    return new CreateGroupFlow().groupId(GROUP1_ID).studentId(STUDENT2_ID).moveType(LEAVE);
  }
}
