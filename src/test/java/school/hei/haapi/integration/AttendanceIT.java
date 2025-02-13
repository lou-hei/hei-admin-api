package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.AttendanceMovementType.IN;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.LATE;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.MISSING;
import static school.hei.haapi.endpoint.rest.model.PlaceEnum.ANDRAHARO;
import static school.hei.haapi.endpoint.rest.model.PlaceEnum.IVANDRY;
import static school.hei.haapi.integration.StudentIT.*;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.StudentIT.student2;
import static school.hei.haapi.integration.conf.TestUtils.*;
import static school.hei.haapi.model.User.Status.ENABLED;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.model.CheckAttendanceTriggered;
import school.hei.haapi.endpoint.rest.api.AttendanceApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.*;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.UserRepository;
import school.hei.haapi.service.event.CheckAttendanceTriggeredService;

@Testcontainers
@AutoConfigureMockMvc
class AttendanceIT extends FacadeITMockedThirdParties {
  private static final Instant DEFAULT_FROM = Instant.parse("2021-08-07T07:30:00.00Z");
  private static final Instant DEFAULT_TO = Instant.parse("2021-11-09T07:30:00.00Z");
  @Autowired CheckAttendanceTriggeredService checkAttendanceTriggeredService;
  @Autowired UserRepository userRepository;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    checkAttendanceTriggeredService.accept(new CheckAttendanceTriggered());
    setUpS3Service(fileService, student1());

    User student = userRepository.findByRef(student1().getRef());
    student.setStatus(ENABLED);
    userRepository.save(student);
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
            List.of(MISSING, LATE));
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
            List.of(MISSING));
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
            1, 10, null, null, null, DEFAULT_FROM, DEFAULT_TO, List.of(MISSING));
    assertEquals(2, actualWithStudentMissing.size());
    assertTrue(
        actualWithStudentMissing.containsAll(List.of(attendance6Missing(), attendance5Missing())));

    // GET
    // /attendance?page=1&page_size=10&attendance_statuses=LATE,MISSING&from={DEFAULT_FROM}&to={DEFAULT_TO}
    List<StudentAttendance> actualWithStudentMissingAndLate =
        api.getStudentsAttendance(
            1, 10, null, null, null, DEFAULT_FROM, DEFAULT_TO, List.of(MISSING, LATE));
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
            List.of(MISSING));
    assertEquals(1, actualWithCourse2IdAndMissing.size());
    assertTrue(actualWithCourse2IdAndMissing.contains(attendance6Missing()));
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
            List.of(LATE));
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
            .place(ANDRAHARO)
            .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"))
            .student(student1())
            .attendanceMovementType(AttendanceMovementType.IN);

    assertEquals(expected.getCreatedAt(), actual.getFirst().getCreatedAt());
    assertEquals(
        expected.getAttendanceMovementType(), actual.getFirst().getAttendanceMovementType());
    assertEquals(expected.getPlace(), actual.getFirst().getPlace());
    assertEquals(expected.getStudent(), actual.getFirst().getStudent());
  }

  @Test
  void manager_create_attendance_with_no_student_id() {
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
        .place(ANDRAHARO)
        .isLate(false)
        .courseSession(courseSession1())
        .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"));
  }

  public static StudentAttendance attendance2Ok() {
    return new StudentAttendance()
        .id("attendance2_id")
        .student(student1())
        .lateOf(0)
        .place(ANDRAHARO)
        .isLate(false)
        .courseSession(courseSession2())
        .createdAt(Instant.parse("2021-08-08T14:15:00.00Z"))
        .attendanceMovementType(IN);
  }

  public static StudentAttendance attendance3Late() {
    return new StudentAttendance()
        .id("attendance3_id")
        .place(IVANDRY)
        .isLate(true)
        .lateOf(35)
        .courseSession(courseSession1())
        .student(student2())
        .createdAt(Instant.parse("2021-11-08T08:35:00.00Z"))
        .attendanceMovementType(IN);
  }

  public static StudentAttendance attendance4Late() {
    return new StudentAttendance()
        .id("attendance4_id")
        .isLate(true)
        .lateOf(15)
        .place(ANDRAHARO)
        .courseSession(courseSession2())
        .student(student2())
        .createdAt(Instant.parse("2021-08-08T15:15:00.00Z"))
        .attendanceMovementType(IN);
  }

  public static StudentAttendance attendance5Missing() {
    return new StudentAttendance()
        .id("attendance5_id")
        .lateOf(0)
        .isLate(false)
        .student(student3())
        .courseSession(courseSession1())
        .createdAt(null)
        .attendanceMovementType(IN);
  }

  public static StudentAttendance attendance6Missing() {
    return new StudentAttendance()
        .id("attendance6_id")
        .lateOf(0)
        .isLate(false)
        .student(student3())
        .courseSession(courseSession2())
        .createdAt(null)
        .attendanceMovementType(IN);
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
        .place(ANDRAHARO)
        .attendanceMovementType(AttendanceMovementType.IN)
        .studentId("student1_id")
        .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"));
  }

  public static CreateAttendanceMovement createAttendanceMovementKo() {
    return new CreateAttendanceMovement()
        .place(ANDRAHARO)
        .attendanceMovementType(AttendanceMovementType.IN)
        .studentId("student_id_ko")
        .createdAt(Instant.parse("2021-11-08T07:30:00.00Z"));
  }
}
