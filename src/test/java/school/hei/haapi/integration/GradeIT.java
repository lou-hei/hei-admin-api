package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.*;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.TeachingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.AwardedCourseExam;
import school.hei.haapi.endpoint.rest.model.CrupdateGrade;
import school.hei.haapi.endpoint.rest.model.Grade;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = GradeIT.ContextInitializer.class)
@AutoConfigureMockMvc
class GradeIT extends MockedThirdParties {

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, GradeIT.ContextInitializer.SERVER_PORT);
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

    List<AwardedCourseExam> actualAwardedCourseExamGrades =
        api.getStudentGrades(STUDENT1_ID, 1, 10);

    assertEquals(5, actualAwardedCourseExamGrades.size());
    assertTrue(actualAwardedCourseExamGrades.contains(awardedCourseExam1()));

    assertTrue(actualAwardedCourseExamGrades.contains(awardedCourseExam2()));
    assertTrue(actualAwardedCourseExamGrades.contains(awardedCourseExam4()));
  }

  @Test
  void teacher_read_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacher1Client);

    List<AwardedCourseExam> actual = api.getStudentGrades(STUDENT1_ID, 1, 10);

    assertEquals(5, actual.size());
    assertTrue(actual.contains(awardedCourseExam1()));
    assertTrue(actual.contains(awardedCourseExam2()));
    assertTrue(actual.contains(awardedCourseExam4()));
  }

  @Test
  void student_read_his_grade_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(student1Client);

    List<AwardedCourseExam> actual = api.getStudentGrades(STUDENT1_ID, 1, 10);

    assertEquals(5, actual.size());
    assertTrue(actual.contains(awardedCourseExam1()));
    assertTrue(actual.contains(awardedCourseExam2()));
    assertTrue(actual.contains(awardedCourseExam4()));
  }

  @Test
  void student_read_other_grade_ko() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(student1Client);
    assertThrowsForbiddenException(() -> api.getStudentGrades(STUDENT2_ID, 1, 10));
    assertThrowsForbiddenException(() -> api.getParticipantGrade(GROUP1_ID, EXAM1_ID));
  }

  //  @Test
  //  void student_read_ko() throws ApiException {
  //    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
  //    TeachingApi api = new TeachingApi(student1Client);
  //    assertThrowsForbiddenException(
  //        () -> api.getExamGrades(GROUP1_ID, EXAM1_ID, AWARDED_COURSE1_ID));
  //  }

  //  void manager_create_grades_ok() throws ApiException {
  //    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
  //    TeachingApi api = new TeachingApi(manager1Client);
  //    List<ExamDetail> actual =
  //        api.createStudentExamGrade(
  //            GROUP1_ID,
  //            AWARDED_COURSE1_ID,
  //            EXAM1_ID,
  //            List.of(createGrade(STUDENT1_ID, EXAM1_ID, AWARDED_COURSE1_ID)));
  //    assertEquals(1, actual.size());
  //  }

  //  void teacher_create_his_exam_grades_ok() throws ApiException {
  //    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
  //    TeachingApi api = new TeachingApi(teacher1Client);
  //    List<ExamDetail> actual =
  //        api.createStudentExamGrade(
  //            GROUP1_ID,
  //            AWARDED_COURSE1_ID,
  //            EXAM1_ID,
  //            List.of(createGrade(STUDENT1_ID, EXAM1_ID, AWARDED_COURSE1_ID)));
  //    assertEquals(1, actual.size());
  //  }

  @Test
  void manager_crupdate_grade_ok() throws ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(managerClient);

    CrupdateGrade newGrade = new CrupdateGrade();
    newGrade.setScore(18.2);
    Grade actualGrade = api.crupdateParticipantGrade(EXAM1_ID, STUDENT3_ID, newGrade);

    Assertions.assertNotNull(actualGrade.getId());
    assertEquals(36.4, actualGrade.getScore());
  }

  @Test
  void manager_crupdate_grade_ko() throws ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(managerClient);

    CrupdateGrade newGrade = new CrupdateGrade();
    newGrade.setScore(28.2);

    ApiException illegalArgumentException = assertThrows(ApiException.class, () -> api.crupdateParticipantGrade(EXAM1_ID, STUDENT3_ID, newGrade));

    String exceptedMessage = "score must be between 0 and 20";
    String actualMessage = illegalArgumentException.getMessage();

    assertTrue(actualMessage.contains(exceptedMessage));
  }

  @Test
  void teacher_crupdate_grade_ok() throws ApiException {
    ApiClient teacherClient = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacherClient);

    CrupdateGrade newGrade = new CrupdateGrade();
    newGrade.setScore(5.25);
    Grade actualGrade = api.crupdateParticipantGrade(EXAM2_ID, STUDENT3_ID, newGrade);

    assertEquals(45.75, actualGrade.getScore());
  }

  @Test
  void student_crupdate_grade_forbidden() {
    ApiClient studentClient = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(studentClient);

    CrupdateGrade newCrupdateGrade = new CrupdateGrade();
    newCrupdateGrade.setScore(90.0);

    assertThrowsForbiddenException(
        () -> api.crupdateParticipantGrade(EXAM1_ID, STUDENT1_ID, newCrupdateGrade));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
