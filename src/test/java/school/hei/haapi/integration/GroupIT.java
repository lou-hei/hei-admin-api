package school.hei.haapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.GroupsApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CreateGroup;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.endpoint.rest.security.cognito.CognitoComponent;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.TestUtils;

import java.util.List;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.isValidUUID;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = GroupIT.ContextInitializer.class)
@AutoConfigureMockMvc
public class GroupIT {

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = 8081;

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }

  private static ApiClient aClient(String token) {
    return TestUtils.aClient(token, ContextInitializer.SERVER_PORT);
  }

  @MockBean
  private CognitoComponent cognitoComponent;

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponent);
  }

  @Test
  void student_can_get_group1() throws ApiException {
    ApiClient student1Client = aClient(TestUtils.STUDENT1_TOKEN);

    GroupsApi api = new GroupsApi(student1Client);
    Group group = api.findGroupById("TODO:school1", TestUtils.GROUP1_ID);

    assertEquals("Name of G1", group.getName());
    assertEquals("G1", group.getRef());
    assertTrue(group.getCreationDatetime().isBefore(now()));
  }

  @Test
  void student_can_get_groups() throws ApiException {
    ApiClient student1Client = aClient(TestUtils.STUDENT1_TOKEN);

    GroupsApi api = new GroupsApi(student1Client);
    List<Group> groups = api.findGroups("TODO:school1");

    assertEquals(2, groups.size());
    assertEquals("group1_id", groups.get(0).getId());
    assertEquals("group2_id", groups.get(1).getId());
  }

  @Test
  void student_cannot_create_groups() {
    ApiClient student1Client = aClient(TestUtils.STUDENT1_TOKEN);

    GroupsApi api = new GroupsApi(student1Client);
    assertThrowsApiException(
        () ->  api.createGroups("TODO:school1", List.of()),
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Only managers can write groups\"}");
  }

  @Test
  void teacher_cannot_create_groups() {
    ApiClient teacher1Client = aClient(TestUtils.TEACHER1_TOKEN);

    GroupsApi api = new GroupsApi(teacher1Client);
    assertThrowsApiException(
        () ->  api.createGroups("TODO:school1", List.of()),
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Only managers can write groups\"}");
  }

  @Test
  void manager_can_create_groups() throws ApiException {
    ApiClient manager1Client = aClient(TestUtils.MANAGER1_TOKEN);
    CreateGroup createGroup3 = new CreateGroup();
    createGroup3.setName("Name of group three");
    createGroup3.setRef("G3");
    CreateGroup createGroup4 = new CreateGroup();
    createGroup4.setName("Name of group four");
    createGroup4.setRef("G4");

    GroupsApi api = new GroupsApi(manager1Client);
    List<Group> createdGroups = api.createGroups("TODO:school1", List.of(createGroup3, createGroup4));

    assertEquals(2, createdGroups.size());
    Group createdGroup3 = createdGroups.get(0);
    assertTrue(isValidUUID(createdGroup3.getId()));
    assertEquals("Name of group three", createdGroup3.getName());
    assertEquals("G3", createdGroup3.getRef());
    assertTrue(createdGroup3.getCreationDatetime().isBefore(now()));
    Group createdGroup4 = createdGroups.get(1);
    assertTrue(isValidUUID(createdGroup4.getId()));
    assertEquals("Name of group four", createdGroup4.getName());
    assertEquals("G4", createdGroup4.getRef());
    assertTrue(createdGroup4.getCreationDatetime().isBefore(now()));
  }
}
