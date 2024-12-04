package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.*;

import java.time.Instant;
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
import school.hei.haapi.endpoint.rest.model.ExamInfo;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ExamIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
class ExamIT extends MockedThirdParties {
  // TODO: some resources are not implemented yet then test failed
  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ExamIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  /*


    //  @Test
    //  void student_read_exam_grades_ko() {
    //    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    //    TeachingApi api = new TeachingApi(student1Client);
    //    assertThrowsForbiddenException(
    //        () -> api.get(GROUP1_ID, EXAM1_ID, AWARDED_COURSE1_ID));
    //  }

    //  @Test
    //  void manager_read_exam_details_ok() throws ApiException {
    //    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    //    TeachingApi api = new TeachingApi(manager1Client);
    //    ExamDetail actual = api.getExamGrades(GROUP1_ID, EXAM1_ID, AWARDED_COURSE1_ID);
    //    assertEquals(examDetail1(), actual);
    //  }
    //
    //  void student_create_or_update_exam_ko() throws ApiException {
    //    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    //    TeachingApi api = new TeachingApi(student1Client);
    //    assertThrowsApiException(
    //        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
    //        () -> api.createOrUpdateExams(GROUP1_ID, AWARDED_COURSE1_ID, List.of(exam1())));
    //  }
  */
  @Test
  void student_read_exam_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(student1Client);
    String exam1Id = exam1().getId();
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getExamOneExamById(exam1Id));
  }

  @Test
  void manager_read_exam_ko() {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);
    String nonExistentExamId = "NON_EXISTENT_EXAM";
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Exam with id #"
            + nonExistentExamId
            + " not found\"}",
        () -> api.getExamOneExamById(nonExistentExamId));
  }

  @Test
  void manager_read_exam_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);
    String exam1Id = exam1().getId();
    ExamInfo actual = api.getExamOneExamById(exam1Id);
    assertDoesNotThrow(() -> api.getExamOneExamById(exam1Id));
    assertEquals(actual, exam1());
  }

  @Test
  void teacher_read_exam_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacher1Client);
    String exam1Id = exam1().getId();
    ExamInfo actual = api.getExamOneExamById(exam1Id);
    assertDoesNotThrow(() -> api.getExamOneExamById(exam1Id));
    assertEquals(actual, exam1());
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);
    List<ExamInfo> actual =
        api.getAllExams(null, null, null, null, Instant.parse("2022-10-09T08:25:24Z"), null, 1, 10);

    assertEquals(5, actual.size());
    assertTrue(actual.contains(exam1()));
    assertTrue(actual.contains(exam2()));
    assertTrue(actual.contains(exam3()));
    assertTrue(actual.contains(exam4()));
    assertTrue(actual.contains(exam5()));
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    TeachingApi api = new TeachingApi(student1Client);
    assertThrowsForbiddenException(
        () -> api.getAllExams(null, null, null, null, null, null, 1, 10));
  }

  @Test
  void teacher_read_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacher1Client);
    List<ExamInfo> actual = api.getAllExams(null, null, "", "", null, null, 1, 10);

    assertEquals(5, actual.size());
    assertTrue(actual.contains(exam1()));
    assertTrue(actual.contains(exam2()));
    assertTrue(actual.contains(exam3()));
    assertTrue(actual.contains(exam4()));
    assertTrue(actual.contains(exam5()));
  }

  @Test
  @DirtiesContext
  void teacher_create_or_update_exam_ok() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    TeachingApi api = new TeachingApi(teacher1Client);
    ExamInfo actualCreat = api.createOrUpdateExamsInfos(createExam1());

    assertEquals(exam1(), actualCreat);
  }

  @Test
  @DirtiesContext
  void manager_create_or_update_exam_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    TeachingApi api = new TeachingApi(manager1Client);
    ExamInfo actualCreat = api.createOrUpdateExamsInfos(createExam1());

    assertEquals(exam1(), actualCreat);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
