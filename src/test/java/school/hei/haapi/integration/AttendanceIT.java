package school.hei.haapi.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.StudentIT.student2;
import static school.hei.haapi.integration.StudentIT.student3;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.awardedCourse1;
import static school.hei.haapi.integration.conf.TestUtils.course1;
import static school.hei.haapi.integration.conf.TestUtils.course2;
import static school.hei.haapi.integration.conf.TestUtils.course3;
import static school.hei.haapi.integration.conf.TestUtils.group1;
import static school.hei.haapi.integration.conf.TestUtils.group2;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.integration.conf.TestUtils.teacher1;
import static school.hei.haapi.integration.conf.TestUtils.teacher2;
import static school.hei.haapi.integration.conf.TestUtils.teacher4;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.model.CheckAttendanceTriggered;
import school.hei.haapi.endpoint.rest.api.AttendanceApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.AttendanceMovementType;
import school.hei.haapi.endpoint.rest.model.AttendanceStatus;
import school.hei.haapi.endpoint.rest.model.AwardedCourse;
import school.hei.haapi.endpoint.rest.model.CourseSession;
import school.hei.haapi.endpoint.rest.model.CreateAttendanceMovement;
import school.hei.haapi.endpoint.rest.model.PlaceEnum;
import school.hei.haapi.endpoint.rest.model.StudentAttendance;
import school.hei.haapi.endpoint.rest.model.StudentAttendanceMovement;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.service.event.CheckAttendanceTriggeredService;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AttendanceIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
class AttendanceIT extends MockedThirdParties {
  private static final Instant DEFAULT_FROM = Instant.parse("2021-08-07T07:30:00.00Z");
  private static final Instant DEFAULT_TO = Instant.parse("2021-11-09T07:30:00.00Z");
  @Autowired CheckAttendanceTriggeredService checkAttendanceTriggeredService;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    checkAttendanceTriggeredService.accept(new CheckAttendanceTriggered());
    setUpS3Service(fileService, student1());
  }

  @Test
  void teacher_read_attendance_with_course_session_criteria() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    AttendanceApi api = new AttendanceApi(teacher1Client);

    // GET
    // /attendance?page=1&page_size=10&courses_ids=course2_id&from={DEFAULT_FROM}&to={DEFAULT_TO}
    List<StudentAttendance> actualWithCourse2Id =
        api.getStudentsAttendance(
            1, 10, List.of("course2_id"), null, null, DEFAULT_FROM, DEFAULT_TO, null);
    assertEquals(3, actualWithCourse2Id.size());
    assertTrue(
        actualWithCourse2Id.containsAll(
            List.of(attendance2Ok(), attendance4Late(), attendance6Missing())));

    // GET
    // /attendance?page=1&page_size=10&courses_ids=course2_id&attendance_statuses=MISSING,LATE&from={DEFAULT_FROM}&to={DEFAULT_TO}
    List<StudentAttendance> actualWithCourse2IdAndMissingAndLate =
        api.getStudentsAttendance(
            1,
            10,
            List.of("course2_id"),
            null,
            null,
            DEFAULT_FROM,
            DEFAULT_TO,
            List.of(AttendanceStatus.MISSING, AttendanceStatus.LATE));
    assertEquals(2, actualWithCourse2IdAndMissingAndLate.size());
    assertTrue(
        actualWithCourse2IdAndMissingAndLate.containsAll(
            List.of(attendance6Missing(), attendance4Late())));

    // GET
    // /attendance?page=1&page_size=10&courses_ids=course1_id,course2_id&attendance_statuses=MISSING&from={DEFAULT_FROM}&to={DEFAULT_TO}
    List<StudentAttendance> actualWithCourse1Idand2IdAndMissing =
        api.getStudentsAttendance(
            1,
            10,
            List.of(course1().getId(), course2().getId()),
            null,
            null,
            DEFAULT_FROM,
            DEFAULT_TO,
            List.of(AttendanceStatus.MISSING));
    assertEquals(2, actualWithCourse1Idand2IdAndMissing.size());
    assertTrue(
        actualWithCourse1Idand2IdAndMissing.containsAll(
            List.of(attendance6Missing(), attendance5Missing())));
  }

  @Test
  void teacher_read_attendance_with_attendance_status() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    AttendanceApi api = new AttendanceApi(teacher1Client);

    // Get
    // /attendance?page=1&page_size=10&attendance_statuses=MISSING&from={DEFAULT_FROM}&to={DEFAULT_TO}
    List<StudentAttendance> actualWithStudentMissing =
        api.getStudentsAttendance(
            1, 10, null, null, null, DEFAULT_FROM, DEFAULT_TO, List.of(AttendanceStatus.MISSING));
    assertEquals(2, actualWithStudentMissing.size());
    assertTrue(
        actualWithStudentMissing.containsAll(List.of(attendance6Missing(), attendance5Missing())));

    // GET
    // /attendance?page=1&page_size=10&attendance_statuses=LATE,MISSING&from={DEFAULT_FROM}&to={DEFAULT_TO}
    List<StudentAttendance> actualWithStudentMissingAndLate =
        api.getStudentsAttendance(
            1,
            10,
            null,
            null,
            null,
            DEFAULT_FROM,
            DEFAULT_TO,
            List.of(AttendanceStatus.MISSING, AttendanceStatus.LATE));
    assertEquals(4, actualWithStudentMissingAndLate.size());
    assertTrue(
        actualWithStudentMissingAndLate.containsAll(
            List.of(
                attendance5Missing(), attendance6Missing(), attendance3Late(), attendance4Late())));

    // GET
    // /attendance?page=1&page_size=10&courses_ids=course2_id&attendance_statuses=MISSING&from={DEFAULT_FROM}&to={DEFAULT_TO}
    List<StudentAttendance> actualWithCourse2IdAndMissing =
        api.getStudentsAttendance(
            1,
            10,
            List.of(course2().getId()),
            null,
            null,
            DEFAULT_FROM,
            DEFAULT_TO,
            List.of(AttendanceStatus.MISSING));
    assertEquals(1, actualWithCourse2IdAndMissing.size());
    assertTrue(actualWithCourse2IdAndMissing.containsAll(List.of(attendance6Missing())));
  }

  @Test
  void teacher_read_attendance_by_teachers_ids() throws ApiException {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    AttendanceApi api = new AttendanceApi(teacher1Client);

    // GET
    // /attendance?page=1&page_size=10&from={DEFAULT_FROM}&to={DEFAULT_TO}&student_key_word=tw&attendance_statuses=LATE&teachers_ids=teacher1_id
    List<StudentAttendance> actualWithStudentKeyowrdAndTeacher1AndAttendanceLate =
        api.getStudentsAttendance(
            1,
            10,
            null,
            List.of(teacher1().getId()),
            "tw",
            DEFAULT_FROM,
            DEFAULT_TO,
            List.of(AttendanceStatus.LATE));
    assertEquals(1, actualWithStudentKeyowrdAndTeacher1AndAttendanceLate.size());
  }

  @Test
  void manager_create_attendance_movement_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    AttendanceApi api = new AttendanceApi(manager1Client);

    List<StudentAttendanceMovement> actual =
        api.createAttendanceMovement(List.of(createAttendanceMovement()));
    StudentAttendanceMovement expected =
        new StudentAttendanceMovement()
            .id("attendance1_id")
            .place(PlaceEnum.ANDRAHARO)
            .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"))
            .student(student1())
            .attendanceMovementType(AttendanceMovementType.IN);

    assertEquals(expected.getCreatedAt(), actual.get(0).getCreatedAt());
    assertEquals(expected.getAttendanceMovementType(), actual.get(0).getAttendanceMovementType());
    assertEquals(expected.getPlace(), actual.get(0).getPlace());
    assertEquals(expected.getStudent(), actual.get(0).getStudent());
  }

  @Test
  void manager_create_attendance_with_no_student_id() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    AttendanceApi api = new AttendanceApi(manager1Client);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"the student with #student_id_ko doesn't"
            + " exist\"}",
        () -> api.createAttendanceMovement(List.of(createAttendanceMovementKo())));
  }

  public static AwardedCourse awardedCourse4() {
    return new AwardedCourse()
        .id("awarded_course4_id")
        .course(course2())
        .group(group1())
        .mainTeacher(teacher4());
  }

  public static AwardedCourse awardedCourse5() {
    return new AwardedCourse()
        .id("awarded_course5_id")
        .course(course3())
        .group(group2())
        .mainTeacher(teacher2());
  }

  public static CourseSession courseSession1() {
    return new CourseSession()
        .id("course_session1_id")
        .awarededCourse(awardedCourse1())
        .begin(Instant.parse("2021-11-08T08:00:00.00Z"))
        .end(Instant.parse("2021-11-08T12:00:00.00Z"));
  }

  public static CourseSession courseSession2() {
    return new CourseSession()
        .id("course_session2_id")
        .awarededCourse(awardedCourse4())
        .begin(Instant.parse("2021-08-08T15:00:00.00Z"))
        .end(Instant.parse("2021-08-08T17:00:00.00Z"));
  }

  public static StudentAttendance attendance1Ok() {
    return new StudentAttendance()
        .id("attendance1_id")
        .lateOf(0)
        .student(student1())
        .place(PlaceEnum.ANDRAHARO)
        .isLate(false)
        .courseSession(courseSession1())
        .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"));
  }

  public static StudentAttendance attendance2Ok() {
    return new StudentAttendance()
        .id("attendance2_id")
        .student(student1())
        .lateOf(0)
        .place(PlaceEnum.ANDRAHARO)
        .isLate(false)
        .courseSession(courseSession2())
        .createdAt(Instant.parse("2021-08-08T14:15:00.00Z"));
  }

  public static StudentAttendance attendance3Late() {
    return new StudentAttendance()
        .id("attendance3_id")
        .place(PlaceEnum.IVANDRY)
        .isLate(true)
        .lateOf(35)
        .courseSession(courseSession1())
        .student(student2())
        .createdAt(Instant.parse("2021-11-08T08:35:00.00Z"));
  }

  public static StudentAttendance attendance4Late() {
    return new StudentAttendance()
        .id("attendance4_id")
        .isLate(true)
        .lateOf(15)
        .place(PlaceEnum.ANDRAHARO)
        .courseSession(courseSession2())
        .student(student2())
        .createdAt(Instant.parse("2021-08-08T15:15:00.00Z"));
  }

  public static StudentAttendance attendance5Missing() {
    return new StudentAttendance()
        .id("attendance5_id")
        .lateOf(0)
        .isLate(false)
        .student(student3())
        .courseSession(courseSession1())
        .createdAt(null);
  }

  public static StudentAttendance attendance6Missing() {
    return new StudentAttendance()
        .id("attendance6_id")
        .lateOf(0)
        .isLate(false)
        .student(student3())
        .courseSession(courseSession2())
        .createdAt(null);
  }

  public static StudentAttendance attendance7Out() {
    return new StudentAttendance()
        .id("attendance7_id")
        .createdAt(Instant.parse("2021-11-08T012:30:00.00Z"))
        .isLate(false)
        .student(student1())
        .courseSession(courseSession1());
  }

  public static CreateAttendanceMovement createAttendanceMovement() {
    return new CreateAttendanceMovement()
        .place(PlaceEnum.ANDRAHARO)
        .attendanceMovementType(AttendanceMovementType.IN)
        .studentId("student1_id")
        .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"));
  }

  public static CreateAttendanceMovement createAttendanceMovementKo() {
    return new CreateAttendanceMovement()
        .place(PlaceEnum.ANDRAHARO)
        .attendanceMovementType(AttendanceMovementType.IN)
        .studentId("student_id_ko")
        .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
