package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.*;

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
import school.hei.haapi.endpoint.rest.model.*;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AwardedCourseIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
class AwardedCourseIT extends MockedThirdParties {

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, AwardedCourseIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);

    AwardedCourse actual = api.getAwardedCoursesByIdAndGroupId(GROUP1_ID, AWARDED_COURSE1_ID);

    List<AwardedCourse> actuals = api.getAllAwardedCourseByGroup(GROUP1_ID, 1, 10);

    List<AwardedCourse> allAwardedCourse =
        api.getAllAwardedCourseByCriteria(null, null, null, null, null);

    List<AwardedCourse> awardedCoursesByTeacher =
        api.getAllAwardedCourseByCriteria(TEACHER1_ID, null, null, null, null);

    List<AwardedCourse> awardedCoursesByCourse =
        api.getAllAwardedCourseByCriteria(null, null, COURSE1_ID, null, null);

    List<AwardedCourse> awardedCoursesAssignedToTeacher =
        api.getAwardedCoursesAssignedToTeacher(TEACHER2_ID, 1, 10);

    assertEquals(awardedCourse1(), actual);

    assertEquals(3, actuals.size());
    assertTrue(actuals.contains(awardedCourse1()));
    assertTrue(actuals.contains(awardedCourse2()));
    assertTrue(actuals.contains(awardedCourse4()));

    assertEquals(5, allAwardedCourse.size());
    assertTrue(allAwardedCourse.contains(awardedCourse1()));
    assertTrue(allAwardedCourse.contains(awardedCourse2()));
    assertTrue(allAwardedCourse.contains(awardedCourse3()));
    assertTrue(allAwardedCourse.contains(awardedCourse4()));

    assertEquals(1, awardedCoursesByTeacher.size());
    assertTrue(awardedCoursesByTeacher.contains(awardedCourse1()));

    assertEquals(3, awardedCoursesByCourse.size());
    assertTrue(awardedCoursesByCourse.contains(awardedCourse1()));
    assertTrue(awardedCoursesByCourse.contains(awardedCourse2()));
    assertTrue(awardedCoursesByCourse.contains(awardedCourse3()));

    assertEquals(3, awardedCoursesAssignedToTeacher.size());
    assertTrue(awardedCoursesAssignedToTeacher.contains(awardedCourse2()));
    assertTrue(awardedCoursesAssignedToTeacher.contains(awardedCourse3()));
  }

  @Test
  void student_read_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(student1Client);

    List<AwardedCourse> awardedCoursesAssignedToTeacher =
        api.getAwardedCoursesAssignedToTeacher(TEACHER2_ID, 1, 10);

    assertEquals(3, awardedCoursesAssignedToTeacher.size());
    assertTrue(awardedCoursesAssignedToTeacher.contains(awardedCourse2()));
    assertTrue(awardedCoursesAssignedToTeacher.contains(awardedCourse3()));
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(student1Client);
    assertThrowsForbiddenException(
        () -> api.getAwardedCoursesByIdAndGroupId(GROUP1_ID, AWARDED_COURSE1_ID));
    assertThrowsForbiddenException(() -> api.getAllAwardedCourseByGroup(GROUP1_ID, 1, 10));
  }

  @Test
  void awarded_courses_by_teacher_id_ko() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacher1Client);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"User with id: "
            + NOT_EXISTING_ID
            + " not found\"}",
        () -> api.getAwardedCoursesAssignedToTeacher(NOT_EXISTING_ID, 1, 10));
  }

  @Test
  void teacher_read_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacher1Client);
    AwardedCourse actual = api.getAwardedCoursesByIdAndGroupId(GROUP1_ID, AWARDED_COURSE1_ID);

    List<AwardedCourse> actuals = api.getAllAwardedCourseByGroup(GROUP1_ID, 1, 10);

    List<AwardedCourse> allAwardedCourse =
        api.getAllAwardedCourseByCriteria(null, null, null, null, null);

    List<AwardedCourse> awardedCoursesByTeacher =
        api.getAllAwardedCourseByCriteria(TEACHER1_ID, null, null, null, null);

    List<AwardedCourse> awardedCoursesByCourse =
        api.getAllAwardedCourseByCriteria(null, null, COURSE1_ID, null, null);

    List<AwardedCourse> awardedCoursesAssignedToTeacher =
        api.getAwardedCoursesAssignedToTeacher(TEACHER2_ID, 1, 10);

    assertEquals(awardedCourse1(), actual);

    assertEquals(3, actuals.size());
    assertTrue(actuals.contains(awardedCourse1()));
    assertTrue(actuals.contains(awardedCourse2()));
    assertTrue(actuals.contains(awardedCourse4()));

    assertEquals(5, allAwardedCourse.size());
    assertTrue(allAwardedCourse.contains(awardedCourse1()));
    assertTrue(allAwardedCourse.contains(awardedCourse2()));
    assertTrue(allAwardedCourse.contains(awardedCourse3()));
    assertTrue(allAwardedCourse.contains(awardedCourse4()));

    assertEquals(1, awardedCoursesByTeacher.size());
    assertTrue(awardedCoursesByTeacher.contains(awardedCourse1()));

    System.out.println(awardedCoursesByCourse.toString());
    assertEquals(3, awardedCoursesByCourse.size());
    assertTrue(awardedCoursesByCourse.contains(awardedCourse1()));
    assertTrue(awardedCoursesByCourse.contains(awardedCourse2()));
    assertTrue(awardedCoursesByCourse.contains(awardedCourse3()));

    assertEquals(3, awardedCoursesAssignedToTeacher.size());
    assertTrue(awardedCoursesAssignedToTeacher.contains(awardedCourse2()));
    assertTrue(awardedCoursesAssignedToTeacher.contains(awardedCourse3()));
  }

  @Test
  void student_create_or_update_ko() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(student1Client);
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createOrUpdateAwardedCourses(GROUP1_ID, List.of(createAwardedCourse())));

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () ->
            api.createOrUpdateAwardedCoursesAssignToTeacher(
                TEACHER2_ID, someAwardedCoursesToCrupdate()));
  }

  @Test
  void teacher_create_or_update_ko() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacher1Client);
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createOrUpdateAwardedCourses(GROUP1_ID, List.of(createAwardedCourse())));

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () ->
            api.createOrUpdateAwardedCoursesAssignToTeacher(
                TEACHER2_ID, someAwardedCoursesToCrupdate()));
  }

  @Test
  @DirtiesContext
  void manager_create_or_update_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);
    int numberOfExamToAdd = 3;
    List<AwardedCourse> actualCreatList =
        api.createOrUpdateAwardedCourses(
            GROUP1_ID, someCreatableCreateAwardedCourseList(numberOfExamToAdd));
    assertEquals(numberOfExamToAdd, actualCreatList.size());

    List<AwardedCourse> awardedCoursesUpdated =
        api.createOrUpdateAwardedCoursesAssignToTeacher(
            TEACHER2_ID, someAwardedCoursesToCrupdate());

    assertTrue(awardedCoursesUpdated.contains(updatedAwardedCourse2()));
    assertEquals(2, awardedCoursesUpdated.size());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
