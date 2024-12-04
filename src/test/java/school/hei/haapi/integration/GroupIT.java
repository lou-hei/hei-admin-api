package school.hei.haapi.integration;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.TeachingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CreateGroup;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = GroupIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
class GroupIT extends MockedThirdParties {

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  public static Group group1() {
    Group group = new Group();
    group.setId("group1_id");
    group.setRef("G1");
    group.setName("GRP21001");
    group.setCreationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
    group.setSize(2);
    return group;
  }

  public static Group group2() {
    Group group = new Group();
    group.setId("group2_id");
    group.setRef("G2");
    group.setName("GRP21002");
    group.setCreationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"));
    group.setSize(1);
    return group;
  }

  public static Group updatedGroup3() {
    Group copyGroup3 = new Group();
    copyGroup3.setId(group3().getId());
    copyGroup3.setRef(group3().getRef());
    copyGroup3.setCreationDatetime(group3().getCreationDatetime());
    copyGroup3.setName(group3().getName());
    copyGroup3.setSize(1);
    return copyGroup3;
  }

  public static Group updatedGroup5() {
    Group copyGroup5 = new Group();
    copyGroup5.setId(group5().getId());
    copyGroup5.setRef(group5().getRef());
    copyGroup5.setCreationDatetime(group5().getCreationDatetime());
    copyGroup5.setName(group5().getName());
    copyGroup5.setSize(1);
    return copyGroup5;
  }

  public static CreateGroup someCreatableGroup(List<String> students) {
    CreateGroup createGroup = new CreateGroup();
    createGroup.setName("Some name");
    createGroup.setRef("GRP21-" + randomUUID());
    createGroup.setStudents(students);
    return createGroup;
  }

  public static Group createGroupToGroup(CreateGroup createGroup) {
    return new Group()
        .id(createGroup.getId())
        .name(createGroup.getName())
        .creationDatetime(createGroup.getCreationDatetime())
        .ref(createGroup.getRef())
        .size(createGroup.getSize() == null ? 0 : createGroup.getSize());
  }

  public static CreateGroup groupToCreateGroup(Group group) {
    return new CreateGroup()
        .id(group.getId())
        .name(group.getName())
        .creationDatetime(group.getCreationDatetime())
        .ref(group.getRef())
        .size(group.getSize());
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void badtoken_read_ko() {
    ApiClient anonymousClient = anApiClient(BAD_TOKEN);

    TeachingApi api = new TeachingApi(anonymousClient);
    assertThrowsForbiddenException(() -> api.getGroups(null, null, 1, 10));
  }

  @Test
  void badtoken_write_ko() {
    ApiClient anonymousClient = anApiClient(BAD_TOKEN);

    TeachingApi api = new TeachingApi(anonymousClient);
    assertThrowsForbiddenException(() -> api.createOrUpdateGroups(List.of()));
  }

  @Test
  void student_read_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);

    TeachingApi api = new TeachingApi(student1Client);
    Group actual1 = api.getGroupById(GROUP1_ID);
    List<Group> actualGroups = api.getGroups(null, null, 1, 10);

    assertEquals(group1(), actual1);
    assertTrue(actualGroups.contains(group1()));
    assertTrue(actualGroups.contains(group2()));
  }

  @Test
  void student_write_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);

    TeachingApi api = new TeachingApi(student1Client);
    assertThrowsForbiddenException(() -> api.createOrUpdateGroups(List.of()));
  }

  @Test
  void teacher_write_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

    TeachingApi api = new TeachingApi(teacher1Client);
    assertThrowsForbiddenException(() -> api.createOrUpdateGroups(List.of()));
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(client);

    List<Group> actualGroups = api.getGroups(null, null, 1, 10);
    assertTrue(actualGroups.contains(group1()));
    assertTrue(actualGroups.contains(group2()));
    assertTrue(actualGroups.contains(updatedGroup3()));

    List<Group> groupsFilteredByRef = api.getGroups("G1", null, 1, 10);
    assertTrue(groupsFilteredByRef.contains(group1()));
    assertFalse(groupsFilteredByRef.contains(group2()));
    assertFalse(groupsFilteredByRef.contains(updatedGroup3()));

    List<Group> groupsFilteredByStudentRef = api.getGroups(null, "STD21002", 1, 10);
    assertTrue(groupsFilteredByStudentRef.contains(group1()));
    assertFalse(groupsFilteredByStudentRef.contains(group2()));
    assertFalse(groupsFilteredByStudentRef.contains(group3()));
  }

  @Test
  @DirtiesContext
  void manager_write_create_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    CreateGroup toCreate3 = someCreatableGroup(new ArrayList<>());
    CreateGroup toCreate4 = someCreatableGroup(new ArrayList<>());
    CreateGroup toCreate5 = someCreatableGroup(List.of(STUDENT1_ID, STUDENT2_ID));

    TeachingApi api = new TeachingApi(manager1Client);
    List<Group> created = api.createOrUpdateGroups(List.of(toCreate3, toCreate4));
    List<Group> createdWithStudent = api.createOrUpdateGroups(List.of(toCreate5));
    List<Student> students =
        api.getStudentsByGroupId(createdWithStudent.get(0).getId(), 1, 10, null);

    assertEquals(2, created.size());
    Group created3 = created.get(0);
    assertTrue(isValidUUID(created3.getId()));
    toCreate3.setId(created3.getId());
    assertNotNull(created3.getCreationDatetime());
    toCreate3.setCreationDatetime(created3.getCreationDatetime());

    assertEquals(created3, createGroupToGroup(toCreate3));
    Group created4 = created.get(0);
    assertTrue(isValidUUID(created4.getId()));
    toCreate4.setId(created4.getId());
    assertNotNull(created4.getCreationDatetime());
    toCreate4.setCreationDatetime(created4.getCreationDatetime());
    assertEquals(created4, createGroupToGroup(toCreate3));

    assertEquals(2, students.size());
  }

  @Test
  @DirtiesContext
  void manager_write_update_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);

    List<CreateGroup> ModifyGroups =
        List.of(
            groupToCreateGroup(group1()).name("A new name zero"),
            groupToCreateGroup(group2()).name("A new name zero"));

    List<Group> updated = api.createOrUpdateGroups(ModifyGroups);

    assertEquals(2, updated.size());
    assertTrue(updated.contains(createGroupToGroup(ModifyGroups.get(0))));
    assertTrue(updated.contains(createGroupToGroup(ModifyGroups.get(1))));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
