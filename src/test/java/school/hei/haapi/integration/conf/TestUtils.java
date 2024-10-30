package school.hei.haapi.integration.conf;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.MISSING;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.PRESENT;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.ENABLED;
import static school.hei.haapi.endpoint.rest.model.EventType.COURSE;
import static school.hei.haapi.endpoint.rest.model.EventType.INTEGRATION;
import static school.hei.haapi.endpoint.rest.model.EventType.SEMINAR;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.HARDWARE;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.TUITION;
import static school.hei.haapi.endpoint.rest.model.LetterStatus.PENDING;
import static school.hei.haapi.endpoint.rest.model.LetterStatus.RECEIVED;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.AIRTEL_MONEY;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.MVOLA;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.ORANGE_MONEY;
import static school.hei.haapi.endpoint.rest.model.Observer.RoleEnum.MANAGER;
import static school.hei.haapi.endpoint.rest.model.Observer.RoleEnum.TEACHER;
import static school.hei.haapi.endpoint.rest.model.ProfessionalExperienceFileTypeEnum.WORKER_STUDENT;
import static school.hei.haapi.endpoint.rest.model.Scope.GLOBAL;
import static school.hei.haapi.endpoint.rest.model.Scope.STUDENT;
import static school.hei.haapi.endpoint.rest.model.Sex.F;
import static school.hei.haapi.endpoint.rest.model.Sex.M;
import static school.hei.haapi.endpoint.rest.model.SpecializationField.COMMON_CORE;
import static school.hei.haapi.endpoint.rest.model.UpdatePromotionSGroup.TypeEnum.ADD;
import static school.hei.haapi.endpoint.rest.model.UpdatePromotionSGroup.TypeEnum.REMOVE;
import static school.hei.haapi.endpoint.rest.model.WorkStudyStatus.WORKING;
import static school.hei.haapi.integration.ManagerIT.manager1;
import static school.hei.haapi.integration.MpbsIT.expectedMpbs1;
import static school.hei.haapi.integration.StudentIT.student3;
import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.CRLF;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.function.Executable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.shaded.com.google.common.primitives.Bytes;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Announcement;
import school.hei.haapi.endpoint.rest.model.AnnouncementAuthor;
import school.hei.haapi.endpoint.rest.model.AttendanceStatus;
import school.hei.haapi.endpoint.rest.model.AwardedCourse;
import school.hei.haapi.endpoint.rest.model.AwardedCourseExam;
import school.hei.haapi.endpoint.rest.model.Comment;
import school.hei.haapi.endpoint.rest.model.Coordinates;
import school.hei.haapi.endpoint.rest.model.Course;
import school.hei.haapi.endpoint.rest.model.CreateAnnouncement;
import school.hei.haapi.endpoint.rest.model.CreateAwardedCourse;
import school.hei.haapi.endpoint.rest.model.CreateComment;
import school.hei.haapi.endpoint.rest.model.CreateEvent;
import school.hei.haapi.endpoint.rest.model.CreateFee;
import school.hei.haapi.endpoint.rest.model.CrupdateExam;
import school.hei.haapi.endpoint.rest.model.CrupdateFeeTemplate;
import school.hei.haapi.endpoint.rest.model.CrupdateGrade;
import school.hei.haapi.endpoint.rest.model.CrupdatePromotion;
import school.hei.haapi.endpoint.rest.model.CrupdateStudentFee;
import school.hei.haapi.endpoint.rest.model.CrupdateTeacher;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.Event;
import school.hei.haapi.endpoint.rest.model.EventParticipant;
import school.hei.haapi.endpoint.rest.model.ExamInfo;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.FeeTemplate;
import school.hei.haapi.endpoint.rest.model.GetStudentGrade;
import school.hei.haapi.endpoint.rest.model.Grade;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.endpoint.rest.model.GroupIdentifier;
import school.hei.haapi.endpoint.rest.model.Letter;
import school.hei.haapi.endpoint.rest.model.LetterStudent;
import school.hei.haapi.endpoint.rest.model.Manager;
import school.hei.haapi.endpoint.rest.model.Monitor;
import school.hei.haapi.endpoint.rest.model.Observer;
import school.hei.haapi.endpoint.rest.model.Promotion;
import school.hei.haapi.endpoint.rest.model.Scope;
import school.hei.haapi.endpoint.rest.model.Sex;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.endpoint.rest.model.Teacher;
import school.hei.haapi.endpoint.rest.model.UpdatePromotionSGroup;
import school.hei.haapi.endpoint.rest.model.UserIdentifier;
import school.hei.haapi.endpoint.rest.security.cognito.CognitoComponent;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.service.aws.FileService;
import school.hei.haapi.service.mobileMoney.MobileMoneyApiFacade;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class TestUtils {

  public static final String STUDENT1_ID = "student1_id";
  public static final String STUDENT2_ID = "student2_id";
  public static final String STUDENT3_ID = "student3_id";
  public static final String TEACHER1_ID = "teacher1_id";
  public static final String TEACHER2_ID = "teacher2_id";
  public static final String TEACHER3_ID = "teacher3_id";
  public static final String TEACHER4_ID = "teacher4_id";
  public static final String MONITOR1_ID = "monitor1_id";
  public static final String MANAGER_ID = "manager1_id";
  public static final String GROUP1_ID = "group1_id";
  public static final String GROUP2_ID = "group2_id";
  public static final String FEE1_ID = "fee1_id";
  public static final String FEE2_ID = "fee2_id";
  public static final String FEE3_ID = "fee3_id";
  public static final String FEE4_ID = "fee4_id";
  public static final String FEE5_ID = "fee5_id";
  public static final String FEE6_ID = "fee6_id";
  public static final String PAYMENT1_ID = "payment1_id";
  public static final String PAYMENT2_ID = "payment2_id";
  public static final String PAYMENT4_ID = "payment4_id";
  public static final String COURSE1_ID = "course1_id";
  public static final String COURSE2_ID = "course2_id";
  public static final String COURSE3_ID = "course3_id";
  public static final String COURSE4_ID = "course4_id";
  public static final String AWARDED_COURSE1_ID = "awarded_course1_id";
  public static final String AWARDED_COURSE2_ID = "awarded_course2_id";
  public static final String AWARDED_COURSE3_ID = "awarded_course3_id";
  public static final String AWARDED_COURSE4_ID = "awarded_course4_id";
  public static final String EXAM1_ID = "exam1_id";
  public static final String EXAM2_ID = "exam2_id";
  public static final String EXAM3_ID = "exam3_id";
  public static final String EXAM4_ID = "exam4_id";
  public static final String EXAM5_ID = "exam5_id";
  public static final String GRADE1_ID = "grade1_id";
  public static final String GRADE2_ID = "grade2_id";
  public static final String GRADE3_ID = "grade3_id";
  public static final String GRADE4_ID = "grade4_id";
  public static final String GRADE5_ID = "grade5_id";
  public static final String GRADE6_ID = "grade6_id";
  public static final String GRADE7_ID = "grade7_id";
  public static final String GROUP2_REF = "GRP21002";
  public static final String BAD_TOKEN = "bad_token";
  public static final String STUDENT1_TOKEN = "student1_token";
  public static final String TEACHER1_TOKEN = "teacher1_token";
  public static final String MONITOR1_TOKEN = "monitor1_token";
  public static final String MANAGER1_TOKEN = "manager1_token";
  public static final String SUSPENDED_TOKEN = "suspended_token";
  public static final String FEE_TEMPLATE1_ID = "fee_template1";
  public static final String FEE_TEMPLATE2_ID = "fee_template2";
  public static final String FEE_TEMPLATE1_NAME = "annuel x9";

  public static final String EVENT1_ID = "event1_id";
  public static final String EVENT2_ID = "event2_id";
  public static final String EVENT_PARTICIPANT1_ID = "event_participant1_id";
  public static final String EVENT_PARTICIPANT2_ID = "event_participant2_id";

  public static final String ANNOUNCEMENT1_ID = "announcement1_id";
  public static final String ANNOUNCEMENT2_ID = "announcement2_id";
  public static final String ANNOUNCEMENT3_ID = "announcement3_id";
  public static final String ANNOUNCEMENT4_ID = "announcement4_id";
  public static final String PROMOTION1_ID = "promotion1_id";
  public static final String PROMOTION2_ID = "promotion2_id";
  public static final String PROMOTION3_ID = "promotion3_id";

  public static final String STUDENT8_TOKEN = "student8_token";
  public static final String STUDENT7_ID = "student7_id";
  public static final String STUDENT8_ID = "student8_id";

  public static final String NOT_EXISTING_ID = "not_existing_id";

  public static final String LETTER1_ID = "letter1_id";
  public static final String LETTER2_ID = "letter2_id";
  public static final String LETTER3_ID = "letter3_id";

  public static final String LETTER1_REF = "letter1_ref";
  public static final String LETTER2_REF = "letter2_ref";
  public static final String LETTER3_REF = "letter3_ref";
  public static final String EVENT_PARTICIPANT5_ID = "event_participant5_id";

  public static ApiClient anApiClient(String token, int serverPort) {
    ApiClient client = new ApiClient();
    client.setScheme("http");
    client.setHost("localhost");
    client.setPort(serverPort);
    client.setRequestInterceptor(
        httpRequestBuilder -> httpRequestBuilder.header("Authorization", "Bearer " + token));
    return client;
  }

  public static void setUpMobilePaymentApi(MobileMoneyApiFacade mobilePaymentApi) {
    when(mobilePaymentApi.getByTransactionRef(MVOLA, "psp2_id")).thenReturn(psp2Verification());
    when(mobilePaymentApi.getByTransactionRef(ORANGE_MONEY, "psp2_id"))
        .thenThrow(school.hei.haapi.model.exception.ApiException.class);
    when(mobilePaymentApi.getByTransactionRef(AIRTEL_MONEY, "psp2_id"))
        .thenThrow(school.hei.haapi.model.exception.ApiException.class);
  }

  public static void setUpCognito(CognitoComponent cognitoComponent) {
    when(cognitoComponent.getEmailByIdToken(BAD_TOKEN)).thenReturn(null);
    when(cognitoComponent.getEmailByIdToken(STUDENT1_TOKEN)).thenReturn("test+ryan@hei.school");
    when(cognitoComponent.getEmailByIdToken(MONITOR1_TOKEN)).thenReturn("test+monitor@hei.school");
    when(cognitoComponent.getEmailByIdToken(STUDENT8_TOKEN))
        .thenReturn("test+repeating2@hei" + ".school");
    when(cognitoComponent.getEmailByIdToken(TEACHER1_TOKEN)).thenReturn("test+teacher1@hei.school");
    when(cognitoComponent.getEmailByIdToken(MANAGER1_TOKEN)).thenReturn("test+manager1@hei.school");
    when(cognitoComponent.getEmailByIdToken(SUSPENDED_TOKEN))
        .thenReturn("test+suspended@hei.school");
  }

  public static void setUpS3Service(FileService fileService, Student user) {
    when(fileService.getPresignedUrl(user.getRef(), 180L)).thenReturn(user.getRef());
    when(fileService.getFileExtension(any())).thenCallRealMethod();
  }

  public static void setUpS3Service(FileService fileService, Teacher user) {
    when(fileService.getPresignedUrl(user.getRef(), 180L)).thenReturn(user.getRef());
  }

  public static void setUpS3Service(FileService fileService, Manager user) {
    when(fileService.getPresignedUrl(user.getRef(), 180L)).thenReturn(user.getRef());
  }

  public static void setUpEventBridge(EventBridgeClient eventBridgeClient) {
    when(eventBridgeClient.putEvents((PutEventsRequest) any()))
        .thenReturn(PutEventsResponse.builder().build());
  }

  public static void assertThrowsApiException(String expectedBody, Executable executable) {
    ApiException apiException = assertThrows(ApiException.class, executable);
    assertEquals(expectedBody, apiException.getResponseBody());
  }

  public static void assertThrowsForbiddenException(Executable executable) {
    ApiException apiException = assertThrows(ApiException.class, executable);
    String responseBody = apiException.getResponseBody();
    assertEquals(
        "{" + "\"type\":\"403 FORBIDDEN\"," + "\"message\":\"Access is denied\"}", responseBody);
  }

  public static File getMockedFile(String fileName, String extension) {
    try {
      Resource resource = new ClassPathResource("mock/" + fileName + extension);
      File file = resource.getFile();
      return file;
    } catch (IOException e) {
      throw new school.hei.haapi.model.exception.ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  public static byte[] getMockedFileAsByte(String fileName, String extension) {
    try {
      File file = getMockedFile(fileName, extension);
      return FileUtils.readFileToByteArray(file);
    } catch (IOException ioException) {
      throw new school.hei.haapi.model.exception.ApiException(
          SERVER_EXCEPTION, ioException.getMessage());
    }
  }

  public static TransactionDetails psp2Verification() {
    return TransactionDetails.builder()
        .pspDatetimeTransactionCreation(Instant.parse("2021-11-08T08:25:24.00Z"))
        .pspTransactionRef("psp2_id")
        .pspTransactionAmount(300000)
        .build();
  }

  public static CrupdateTeacher someCreatableTeacher() {
    return new CrupdateTeacher()
        .firstName("Some")
        .lastName("User")
        .email(randomUUID() + "@hei.school")
        .ref("TCR21-" + randomUUID())
        .phone("0332511129")
        .status(EnableStatus.ENABLED)
        .sex(Sex.M)
        .birthDate(LocalDate.parse("2000-01-01"))
        .entranceDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .coordinates(coordinatesWithNullValues())
        .address("Adr X");
  }

  public static CreateFee creatableFee1() {
    return new CreateFee()
        .type(TUITION)
        .totalAmount(5000)
        .comment("Comment")
        .dueDatetime(Instant.parse("2021-12-08T08:25:24.00Z"));
  }

  public static CrupdateStudentFee creatableStudentFee() {
    return new CrupdateStudentFee()
        .studentId(STUDENT1_ID)
        .type(TUITION)
        .totalAmount(5000)
        .comment("Comment")
        .dueDatetime(Instant.parse("2021-12-08T08:25:24.00Z"));
  }

  public static CrupdateStudentFee updatableStudentFee() {
    return new CrupdateStudentFee()
        .id(FEE3_ID)
        .studentId(STUDENT1_ID)
        .type(TUITION)
        .totalAmount(5000)
        .comment("Updated comment")
        .creationDatetime(Instant.parse("2022-12-08T08:25:24.00Z"))
        .dueDatetime(Instant.parse("2021-12-09T08:25:24.00Z"));
  }

  public static Group createGroup() {
    return new Group()
        .name("Collaborative work like GWSP")
        .ref("created")
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
  }

  public static Course createCourse(String code) {
    return new Course().code(code).name("Collaborative work like GWSP").credits(12).totalHours(5);
  }

  public static CreateAwardedCourse updateAwardedCourse1() {
    return new CreateAwardedCourse()
        .id(AWARDED_COURSE1_ID)
        .courseId("course1_id")
        .groupId("group1_id")
        .mainTeacherId("teacher1_id");
  }

  public static CreateAwardedCourse createAwardedCourse() {
    return new CreateAwardedCourse()
        .courseId("course2_id")
        .groupId("group2_id")
        .mainTeacherId("teacher2_id");
  }

  public static AwardedCourse updatedAwardedCourse2() {
    return new AwardedCourse()
        .id(AWARDED_COURSE2_ID)
        .course(course2())
        .group(group2())
        .mainTeacher(teacher2());
  }

  public static List<CreateAwardedCourse> someAwardedCoursesToCrupdate() {
    return List.of(
        new CreateAwardedCourse()
            .id(AWARDED_COURSE2_ID)
            .courseId("course2_id")
            .groupId("group2_id")
            .mainTeacherId("teacher2_id"),
        new CreateAwardedCourse()
            .courseId("course2_id")
            .groupId("group1_id")
            .mainTeacherId("teacher2_id"));
  }

  public static ExamInfo createExam() {
    return new ExamInfo()
        .coefficient(10)
        .title("createExam")
        .examinationDate(Instant.parse("2021-11-08T08:25:24.00Z"))
        .awardedCourse(awardedCourse1());
  }

  public static CrupdateGrade createGrade() {
    return new CrupdateGrade().score(20.0);
  }

  public static List<CrupdateTeacher> someCreatableTeacherList(int nbOfTeacher) {
    List<CrupdateTeacher> teacherList = new ArrayList<>();
    for (int i = 0; i < nbOfTeacher; i++) {
      teacherList.add(someCreatableTeacher());
    }
    return teacherList;
  }

  public static List<Group> someCreatableGroupList(int nbOfGroup) {
    List<Group> groupList = new ArrayList<>();
    for (int i = 0; i < nbOfGroup; i++) {
      groupList.add(createGroup());
    }
    return groupList;
  }

  public static List<Course> someCreatableCourseList(int nbOfCourse) {
    List<Course> courseList = new ArrayList<>();
    for (int i = 0; i < nbOfCourse; i++) {
      courseList.add(createCourse("ToAdd" + i));
    }
    return courseList;
  }

  public static List<CreateAwardedCourse> someCreatableCreateAwardedCourseList(
      int nbOfCreateAwardedCourse) {
    List<CreateAwardedCourse> createAwardedCourseList = new ArrayList<>();
    for (int i = 0; i < nbOfCreateAwardedCourse; i++) {
      createAwardedCourseList.add(createAwardedCourse());
    }
    return createAwardedCourseList;
  }

  public static List<ExamInfo> someCreatableExamInfoList(int nbOfExamInfo) {
    List<ExamInfo> examInfoList = new ArrayList<>();
    for (int i = 0; i < nbOfExamInfo; i++) {
      examInfoList.add(createExam());
    }
    return examInfoList;
  }

  public static Group group1() {
    return new Group()
        .id(GROUP1_ID)
        .ref("G1")
        .name("GRP21001")
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .size(2);
  }

  public static Group group2() {
    return new Group()
        .id(GROUP2_ID)
        .ref("G2")
        .name("GRP21002")
        .creationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"))
        .size(1);
  }

  public static Group group3() {
    return new Group()
        .id("group3_id")
        .ref("H1")
        .name("GRP22001")
        .creationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"))
        .size(0);
  }

  public static Group group4() {
    return new Group()
        .id("group4_id")
        .ref("H2")
        .name("GRP22002")
        .creationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"))
        .size(0);
  }

  public static Group group5() {
    return new Group()
        .id("group5_id")
        .ref("J1")
        .name("GRP23001")
        .creationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"))
        .size(0);
  }

  public static Student student1() {
    Student student = new Student();
    student.setId("student1_id");
    student.setFirstName("Ryan");
    student.setLastName("Andria");
    student.setEmail("test+ryan@hei.school");
    student.setRef("STD21001");
    student.setPhone("0322411123");
    student.setStatus(ENABLED);
    student.setSex(M);
    student.setBirthDate(LocalDate.parse("2000-01-01"));
    student.setEntranceDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
    student.setAddress("Adr 1");
    student.setNic("");
    student.setSpecializationField(COMMON_CORE);
    student.setBirthPlace("");
    student.setCoordinates(new Coordinates().longitude(-123.123).latitude(123.0));
    student.setHighSchoolOrigin("Lycée Andohalo");
    student.setWorkStudyStatus(WORKING);
    student.setProfessionalExperience(WORKER_STUDENT);
    student.setCommitmentBeginDate(Instant.parse("2021-11-08T08:25:24Z"));
    student.setGroups(List.of(group1(), group2()));
    student.setIsRepeatingYear(false);
    return student;
  }

  public static Student student2() {
    Student student = new Student();
    student.setId("student2_id");
    student.setFirstName("Two");
    student.setLastName("Student");
    student.setEmail("test+student2@hei.school");
    student.setRef("STD21002");
    student.setPhone("0322411124");
    student.setStatus(ENABLED);
    student.setSex(F);
    student.setBirthDate(LocalDate.parse("2000-01-02"));
    student.setEntranceDatetime(Instant.parse("2021-11-09T08:26:24.00Z"));
    student.setAddress("Adr 2");
    student.setBirthPlace("");
    student.setNic("");
    student.setSpecializationField(COMMON_CORE);
    student.setCoordinates(new Coordinates().longitude(255.255).latitude(-255.255));
    student.setHighSchoolOrigin("Lycée Andohalo");
    student.setWorkStudyStatus(WORKING);
    student.setProfessionalExperience(WORKER_STUDENT);
    student.setCommitmentBeginDate(Instant.parse("2021-11-08T08:25:24.00Z"));
    student.setGroups(List.of(group1()));
    student.setIsRepeatingYear(false);
    return student;
  }

  public static Teacher teacher1() {
    return new Teacher()
        .id("teacher1_id")
        .firstName("One")
        .lastName("Teacher")
        .email("test+teacher1@hei.school")
        .ref("TCR21001")
        .phone("0322411125")
        .status(EnableStatus.ENABLED)
        .sex(Sex.F)
        .birthDate(LocalDate.parse("1990-01-01"))
        .entranceDatetime(Instant.parse("2021-10-08T08:27:24.00Z"))
        .nic("")
        .birthPlace("")
        .coordinates(new Coordinates().longitude(999.999).latitude(999.999))
        .address("Adr 3");
  }

  public static Teacher teacher2() {
    return new Teacher()
        .id("teacher2_id")
        .firstName("Two")
        .lastName("Teacher")
        .email("test+teacher2@hei.school")
        .ref("TCR21002")
        .phone("0322411126")
        .status(EnableStatus.ENABLED)
        .sex(Sex.M)
        .birthDate(LocalDate.parse("1990-01-02"))
        .entranceDatetime(Instant.parse("2021-10-09T08:28:24Z"))
        .nic("")
        .birthPlace("")
        .address("Adr 4")
        .coordinates(coordinatesWithNullValues());
  }

  public static Teacher teacher3() {
    return new Teacher()
        .id("teacher3_id")
        .firstName("Three")
        .lastName("Teach")
        .email("test+teacher3@hei.school")
        .ref("TCR21003")
        .phone("0322411126")
        .status(EnableStatus.ENABLED)
        .sex(Sex.M)
        .birthDate(LocalDate.parse("1990-01-02"))
        .entranceDatetime(Instant.parse("2021-10-09T08:28:24Z"))
        .nic("")
        .birthPlace("")
        .address("Adr 4");
  }

  public static Teacher teacher4() {
    return new Teacher()
        .id(TEACHER4_ID)
        .firstName("Four")
        .lastName("Binary")
        .email("test+teacher4@hei.school")
        .ref("TCR21004")
        .phone("0322411426")
        .status(EnableStatus.ENABLED)
        .sex(Sex.F)
        .birthDate(LocalDate.parse("1990-01-04"))
        .entranceDatetime(Instant.parse("2021-10-09T08:28:24Z"))
        .nic("")
        .birthPlace("")
        .address("Adr 5")
        .coordinates(coordinatesWithNullValues());
  }

  public static Monitor monitor1() {
    return new Monitor()
        .id(MONITOR1_ID)
        .firstName("Monitor")
        .lastName("One")
        .email("test+monitor@hei.school")
        .ref("MTR21001")
        .phone("0322411123")
        .status(EnableStatus.ENABLED)
        .sex(Sex.M)
        .birthDate(LocalDate.parse("2000-01-01"))
        .entranceDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .nic("")
        .birthPlace("")
        .address("Adr 1")
        .coordinates(new Coordinates().longitude(-123.123).latitude(123.0))
        .highSchoolOrigin("Lycée Andohalo");
  }

  public static Fee fee1() {
    return new Fee()
        .id(FEE1_ID)
        .studentId(STUDENT1_ID)
        .studentRef("STD21001")
        .status(PAID)
        .type(TUITION)
        .totalAmount(5000)
        .remainingAmount(0)
        .comment("Comment")
        .mpbs(expectedMpbs1())
        .updatedAt(Instant.parse("2023-02-08T08:30:24Z"))
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .dueDatetime(Instant.parse("2021-12-08T08:25:24.00Z"));
  }

  public static Fee fee2() {
    return new Fee()
        .id(FEE2_ID)
        .studentId(STUDENT1_ID)
        .studentRef("STD21001")
        .status(PAID)
        .type(HARDWARE)
        .totalAmount(5000)
        .remainingAmount(0)
        .comment("Comment")
        .updatedAt(Instant.parse("2023-02-08T08:30:24Z"))
        .creationDatetime(Instant.parse("2021-11-10T08:25:24.00Z"))
        .dueDatetime(Instant.parse("2021-12-10T08:25:24.00Z"));
  }

  public static Fee fee3() {
    return new Fee()
        .id(FEE3_ID)
        .studentId(STUDENT1_ID)
        .studentRef("STD21001")
        .status(LATE)
        .type(TUITION)
        .totalAmount(5000)
        .remainingAmount(5000)
        .comment("Comment")
        .updatedAt(Instant.parse("2023-02-08T08:30:24Z"))
        .creationDatetime(Instant.parse("2022-12-08T08:25:24.00Z"))
        .dueDatetime(Instant.parse("2021-12-09T08:25:24.00Z"));
  }

  public static Fee fee4() {
    return new Fee()
        .id(FEE4_ID)
        .studentId(STUDENT2_ID)
        .status(LATE)
        .type(TUITION)
        .totalAmount(5000)
        .remainingAmount(5000)
        .studentRef("STD21002")
        .comment("Comment")
        .updatedAt(Instant.parse("2023-02-08T08:30:24.00Z"))
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .dueDatetime(Instant.parse("2021-12-09T08:25:25.00Z"));
  }

  public static Course course1() {
    return new Course().id(COURSE1_ID).code("PROG1").credits(6).totalHours(20).name("Algorithmics");
  }

  public static Course course2() {
    return new Course().id(COURSE2_ID).code("PROG3").credits(6).totalHours(24).name("Advanced OOP");
  }

  public static Course course3() {
    return new Course()
        .id(COURSE3_ID)
        .code("IA2")
        .credits(null)
        .totalHours(null)
        .name("Implemented IA");
  }

  public static Course course4() {
    return new Course()
        .id(COURSE4_ID)
        .code("donne1")
        .credits(4)
        .totalHours(30)
        .name("relational data base");
  }

  public static AwardedCourse awardedCourse1() {
    return new AwardedCourse()
        .id(AWARDED_COURSE1_ID)
        .course(course1())
        .group(group1())
        .mainTeacher(teacher1());
  }

  public static AwardedCourse awardedCourse2() {
    return new AwardedCourse()
        .id(AWARDED_COURSE2_ID)
        .course(course1())
        .group(group1())
        .mainTeacher(teacher2());
  }

  public static AwardedCourse awardedCourse3() {
    return new AwardedCourse()
        .id(AWARDED_COURSE3_ID)
        .course(course1())
        .group(group2())
        .mainTeacher(teacher2());
  }

  public static AwardedCourse awardedCourse4() {
    return new AwardedCourse()
        .id(AWARDED_COURSE4_ID)
        .course(course2())
        .group(group1())
        .mainTeacher(teacher4());
  }

  public static CrupdateExam createExam1() {
    return new CrupdateExam()
        .id(EXAM1_ID)
        .coefficient(2)
        .title("Algorithmics")
        .awardedCourseId(awardedCourse1().getId())
        .examinationDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static ExamInfo exam1() {
    return new ExamInfo()
        .id(EXAM1_ID)
        .coefficient(2)
        .title("Algorithmics")
        .awardedCourse(awardedCourse1())
        .examinationDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static ExamInfo exam2() {
    return new ExamInfo()
        .id(EXAM2_ID)
        .coefficient(3)
        .title("Algorithmics final")
        .awardedCourse(awardedCourse1())
        .examinationDate(Instant.parse("2022-11-09T08:25:24Z"));
  }

  public static ExamInfo exam3() {
    return new ExamInfo()
        .id(EXAM3_ID)
        .coefficient(2)
        .title("Algorithmics")
        .awardedCourse(awardedCourse3())
        .examinationDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static ExamInfo exam4() {
    return new ExamInfo()
        .id(EXAM4_ID)
        .coefficient(3)
        .title("Algorithmics2")
        .awardedCourse(awardedCourse2())
        .examinationDate(Instant.parse("2022-11-09T08:25:24Z"));
  }

  public static ExamInfo exam5() {
    return new ExamInfo()
        .id(EXAM5_ID)
        .coefficient(1)
        .title("Prog2 final")
        .awardedCourse(awardedCourse4())
        .examinationDate(Instant.parse("2022-12-09T08:25:24Z"));
  }

  public static Grade grade1() {
    return new Grade()
        .id(GRADE1_ID)
        .score(8.0)
        .createdAt(Instant.parse("2022-10-09T08:25:24Z"))
        .updateDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static Grade grade2() {
    return new Grade()
        .id(GRADE2_ID)
        .score(5.0)
        .createdAt(Instant.parse("2022-11-09T08:25:24Z"))
        .updateDate(Instant.parse("2022-11-09T08:25:24Z"));
  }

  public static Grade grade3() {
    return new Grade()
        .id(GRADE3_ID)
        .score(11.0)
        .createdAt(Instant.parse("2022-11-09T08:25:24Z"))
        .updateDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static Grade grade4() {
    return new Grade()
        .id(GRADE4_ID)
        .score(15.0)
        .createdAt(Instant.parse("2022-10-09T08:25:24Z"))
        .updateDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static Grade grade5() {
    return new Grade()
        .id(GRADE5_ID)
        .score(12.0)
        .createdAt(Instant.parse("2022-10-09T08:25:24Z"))
        .updateDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static Grade grade6() {
    return new Grade()
        .id(GRADE6_ID)
        .score(18.0)
        .createdAt(Instant.parse("2022-11-09T08:25:24Z"))
        .updateDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static Grade grade7() {
    return new Grade()
        .id(GRADE7_ID)
        .score(20.0)
        .createdAt(Instant.parse("2022-10-09T08:25:24Z"))
        .updateDate(Instant.parse("2022-10-09T08:25:24Z"));
  }

  public static GetStudentGrade studentExamGrade1() {
    return new GetStudentGrade().grade(grade1()).student(student1());
  }

  public static GetStudentGrade studentExamGrade2() {
    return new GetStudentGrade().grade(grade2()).student(student1());
  }

  public static GetStudentGrade studentExamGrade3() {
    return new GetStudentGrade().grade(grade3()).student(student1());
  }

  public static GetStudentGrade studentExamGrade4() {
    return new GetStudentGrade().grade(grade4()).student(student1());
  }

  public static GetStudentGrade studentExamGrade5() {
    return new GetStudentGrade().grade(grade5()).student(student1());
  }

  public static GetStudentGrade studentGrade1() {
    return new GetStudentGrade().grade(grade1()).student(student1());
  }

  public static GetStudentGrade studentGrade2() {
    return new GetStudentGrade().grade(grade2());
  }

  public static GetStudentGrade studentGrade3() {
    return new GetStudentGrade().grade(grade3());
  }

  public static GetStudentGrade studentGrade4() {
    return new GetStudentGrade().grade(grade4());
  }

  public static GetStudentGrade studentGrade5() {
    return new GetStudentGrade().grade(grade5());
  }

  public static GetStudentGrade studentGrade6() {
    return new GetStudentGrade().grade(grade6());
  }

  public static GetStudentGrade studentGrade7() {
    return new GetStudentGrade().grade(grade7());
  }

  public static FeeTemplate feeTemplate1() {
    return new FeeTemplate()
        .id(FEE_TEMPLATE1_ID)
        .name(FEE_TEMPLATE1_NAME)
        .numberOfPayments(9)
        .amount(200000)
        .type(TUITION)
        .creationDatetime(Instant.parse("2022-11-08T08:25:24.00Z"));
  }

  public static FeeTemplate feeTemplate3() {
    return new FeeTemplate()
        .id("fee_template3")
        .name("Keyboard")
        .numberOfPayments(1)
        .amount(1000)
        .creationDatetime(Instant.parse("2022-11-08T08:25:24.00Z"))
        .type(HARDWARE);
  }

  public static CrupdateFeeTemplate createFeeTemplate2() {
    return new CrupdateFeeTemplate()
        .id(FEE_TEMPLATE2_ID)
        .name("annuel x1")
        .numberOfPayments(1)
        .amount(10000)
        .type(TUITION);
  }

  public static CrupdateFeeTemplate updateFeeTemplate1() {
    return new CrupdateFeeTemplate()
        .id(feeTemplate1().getId())
        .amount(1000)
        .numberOfPayments(1)
        .type(feeTemplate1().getType())
        .name(feeTemplate1().getName());
  }

  public static FeeTemplate feeTemplate2() {
    return new FeeTemplate()
        .id(FEE_TEMPLATE2_ID)
        .name("annuel x1")
        .numberOfPayments(1)
        .amount(10000)
        .type(TUITION);
  }

  //  public static ExamDetail examDetail1() {
  //    return new ExamDetail()
  //        .id(exam1().getId())
  //        .title(exam1().getTitle())
  //        .examinationDate(exam1().getExaminationDate())
  //        .coefficient(exam1().getCoefficient())
  //        .participants(List.of(studentGrade1(), studentGrade7()));
  //  }

  public static AwardedCourseExam awardedCourseExam1() {
    return new AwardedCourseExam()
        .id(AWARDED_COURSE1_ID)
        .mainTeacher(awardedCourse1().getMainTeacher())
        .course(awardedCourse1().getCourse())
        .group(awardedCourse1().getGroup())
        .exams(List.of(studentExamGrade1(), studentExamGrade2()));
  }

  public static AwardedCourseExam awardedCourseExam2() {
    return new AwardedCourseExam()
        .id(AWARDED_COURSE2_ID)
        .mainTeacher(awardedCourse2().getMainTeacher())
        .course(awardedCourse2().getCourse())
        .group(awardedCourse2().getGroup())
        .exams(List.of(studentExamGrade4()));
  }

  public static AwardedCourseExam awardedCourseExam3() {
    return new AwardedCourseExam()
        .id(AWARDED_COURSE3_ID)
        .mainTeacher(awardedCourse3().getMainTeacher())
        .course(awardedCourse3().getCourse())
        .group(awardedCourse3().getGroup())
        .exams(List.of(studentExamGrade3()));
  }

  public static AwardedCourseExam awardedCourseExam4() {
    return new AwardedCourseExam()
        .id(AWARDED_COURSE4_ID)
        .mainTeacher(awardedCourse4().getMainTeacher())
        .course(awardedCourse4().getCourse())
        .group(awardedCourse4().getGroup())
        .exams(List.of(studentExamGrade5()));
  }

  public static HttpResponse<InputStream> uploadProfilePicture(
      Integer serverPort, String token, String subjectId, String resource)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();

    String basePath = "http://localhost:" + serverPort;

    String boundary = "---------------------------" + System.currentTimeMillis();
    String contentTypeHeader = "multipart/form-data; boundary=" + boundary;

    File file = getMockedFile("img", ".png");

    String requestBodyPrefix =
        "--"
            + boundary
            + CRLF
            + "Content-Disposition: form-data; name=\"file_to_upload\"; filename=\""
            + file.getName()
            + "\""
            + CRLF
            + "Content-Type: image/png"
            + CRLF
            + CRLF;
    byte[] fileBytes = Files.readAllBytes(Paths.get(file.getPath()));
    String requestBodySuffix = CRLF + "--" + boundary + "--" + CRLF;

    byte[] requestBody =
        Bytes.concat(requestBodyPrefix.getBytes(), fileBytes, requestBodySuffix.getBytes());
    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromUri(
            URI.create(basePath + String.format("/%s/%s/picture/raw", resource, subjectId)));
    InputStream requestBodyStream = new ByteArrayInputStream(requestBody);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(uriComponentsBuilder.build().toUri())
            .header("Content-Type", contentTypeHeader)
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> requestBodyStream))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
  }

  public static HttpResponse<InputStream> uploadLetter(
      Integer serverPort,
      String token,
      String subjectId,
      String description,
      String filename,
      String feeId,
      Integer amount,
      String eventParticipantId)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();

    String basePath = "http://localhost:" + serverPort;

    String boundary = "---------------------------" + System.currentTimeMillis();
    String contentTypeHeader = "multipart/form-data; boundary=" + boundary;

    File file = getMockedFile("img", ".png");

    String filePart =
        "--"
            + boundary
            + CRLF
            + "Content-Disposition: form-data; name=\"file_to_upload\"; filename=\""
            + file.getName()
            + "\""
            + CRLF
            + "Content-Type: image/png"
            + CRLF
            + CRLF;
    byte[] fileBytes = Files.readAllBytes(Paths.get(file.getPath()));
    String requestBodySuffix = CRLF + "--" + boundary + "--" + CRLF;

    String descriptionPart =
        "--"
            + boundary
            + CRLF
            + "Content-Disposition: form-data; name=\"description\""
            + CRLF
            + CRLF
            + description
            + CRLF;
    byte[] requestBody =
        Bytes.concat(
            descriptionPart.getBytes(),
            filePart.getBytes(),
            fileBytes,
            requestBodySuffix.getBytes());

    String path = basePath + String.format("/students/%s/letters?filename=%s", subjectId, filename);

    if (Objects.nonNull(feeId)) {
      path = path + "&fee_id=" + feeId;
    }

    if (Objects.nonNull(amount)) {
      path = path + "&amount=" + amount;
    }

    if (Objects.nonNull(eventParticipantId)) {
      path = path + "&event_participant_id=" + eventParticipantId;
    }

    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(URI.create(path));
    InputStream requestBodyStream = new ByteArrayInputStream(requestBody);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(uriComponentsBuilder.build().toUri())
            .header("Content-Type", contentTypeHeader)
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> requestBodyStream))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
  }

  public static Coordinates coordinatesWithNullValues() {
    return new Coordinates().latitude(null).longitude(null);
  }

  public static Coordinates coordinatesWithValues() {
    return new Coordinates().longitude(10.0).latitude(10.0);
  }

  public static Comment comment1() {
    return new Comment()
        .id("comment1_id")
        .content("Good student")
        .subject(student1())
        .observer(observerTeacher1())
        .creationDatetime(Instant.parse("2021-11-09T08:26:24.00Z"));
  }

  public static Comment comment2() {
    return new Comment()
        .id("comment2_id")
        .content("Disruptive student")
        .subject(student1())
        .observer(observerManager1())
        .creationDatetime(Instant.parse("2021-11-09T09:26:24.00Z"));
  }

  public static Comment comment3() {
    return new Comment()
        .id("comment3_id")
        .content("Nothing to say here")
        .subject(student2())
        .observer(observerTeacher1())
        .creationDatetime(Instant.parse("2021-11-09T10:26:24.00Z"));
  }

  public static CreateComment createCommentByManager() {
    return new CreateComment()
        .id("comment4_id")
        .content("Comment about student 1")
        .studentId(STUDENT1_ID)
        .observerId(MANAGER_ID);
  }

  public static CreateComment createCommentByTeacher() {
    return createCommentByManager().observerId(TEACHER1_ID);
  }

  public static Comment commentCreatedByManager() {
    return new Comment()
        .id("comment4_id")
        .content("Comment about student 1")
        .observer(observerManager1())
        .subject(student1());
  }

  public static Comment commentCreatedByTeacher() {
    return commentCreatedByManager().observer(observerTeacher1());
  }

  public static Observer observerTeacher1() {
    return new Observer()
        .id(TEACHER1_ID)
        .firstName(teacher1().getFirstName())
        .lastName(teacher1().getLastName())
        .ref(teacher1().getRef())
        .role(TEACHER);
  }

  public static Observer observerManager1() {
    return new Observer()
        .id(MANAGER_ID)
        .ref(manager1().getRef())
        .role(MANAGER)
        .firstName(manager1().getFirstName())
        .lastName(manager1().getLastName());
  }

  public static UserIdentifier planner1() {
    return new UserIdentifier()
        .id(manager1().getId())
        .ref(manager1().getRef())
        .nic(manager1().getNic())
        .firstName(manager1().getFirstName())
        .lastName(manager1().getLastName())
        .email(manager1().getEmail());
  }

  public static UserIdentifier planner2() {
    return new UserIdentifier()
        .id(manager1().getId())
        .ref(manager1().getRef())
        .nic(manager1().getNic())
        .firstName(manager1().getFirstName())
        .lastName(manager1().getLastName())
        .email(manager1().getEmail());
  }

  public static Event event1() {
    return new Event()
        .id(EVENT1_ID)
        .type(COURSE)
        .course(course1())
        .beginDatetime(Instant.parse("2022-12-20T08:00:00.00Z"))
        .endDatetime(Instant.parse("2022-12-20T10:00:00.00Z"))
        .description("Prog1 course")
        .title("PROG1")
        .planner(planner1())
        .groups(List.of(createGroupIdentifier(group1())));
  }

  public static Event event2() {
    return new Event()
        .id(EVENT2_ID)
        .type(INTEGRATION)
        .planner(planner1())
        .beginDatetime(Instant.parse("2022-12-08T08:00:00.00Z"))
        .endDatetime(Instant.parse("2022-12-08T12:00:00.00Z"))
        .course(null)
        .title("Integration Day")
        .description("HEI students integration day")
        .groups(List.of(createGroupIdentifier(group1()), createGroupIdentifier(group2())));
  }

  public static Event event3() {
    return new Event()
        .id("event3_id")
        .type(SEMINAR)
        .planner(
            new UserIdentifier()
                .id("manager10_id")
                .email("test+manager2@hei.school")
                .ref("MGR21002")
                .nic("")
                .firstName("Two")
                .lastName("Manager"))
        .description("Seminar about Python programming language")
        .beginDatetime(Instant.parse("2022-12-09T08:00:00.00Z"))
        .endDatetime(Instant.parse("2022-12-09T12:00:00.00Z"))
        .title("December Seminar")
        .course(null)
        .groups(List.of());
  }

  public static EventParticipant createParticipant(
      Student student, AttendanceStatus status, String id, String groupName) {
    return new EventParticipant()
        .id(id)
        .studentId(student.getId())
        .firstName(student.getFirstName())
        .lastName(student.getLastName())
        .ref(student.getRef())
        .nic(student.getNic())
        .email(student.getEmail())
        .groupName(groupName)
        .eventStatus(status)
        .letter(List.of());
  }

  public static GroupIdentifier createGroupIdentifier(Group group) {
    return new GroupIdentifier().ref(group.getRef()).name(group.getName()).id(group.getId());
  }

  public static EventParticipant student1MissEvent1() {
    return createParticipant(student1(), MISSING, "event_participant1_id", "GRP21001");
  }

  public static EventParticipant student3AttendEvent1() {
    return createParticipant(student3(), PRESENT, "event_participant2_id", "GRP21001");
  }

  public static EventParticipant student1AttendEvent2() {
    return createParticipant(student1(), PRESENT, "event_participant3_id", "GRP21001");
  }

  public static EventParticipant student2AttendEvent2() {
    return createParticipant(student2(), PRESENT, "event_participant4_id", "GRP21002");
  }

  public static EventParticipant student3MissEvent2() {
    return createParticipant(student3(), MISSING, EVENT_PARTICIPANT5_ID, "GRP21001");
  }

  public static CreateEvent createEventCourse1() {
    return new CreateEvent()
        .id("event4_id")
        .courseId(COURSE1_ID)
        .beginDatetime(Instant.parse("2023-12-08T08:00:00.00Z"))
        .endDatetime(Instant.parse("2023-12-08T10:00:00.00Z"))
        .description("Another Prog1 course")
        .eventType(COURSE)
        .plannerId(MANAGER_ID)
        .groups(List.of(createGroupIdentifier(group1())));
  }

  public static CreateEvent createIntegrationEvent() {
    return new CreateEvent()
        .id("event5_id")
        .courseId(null)
        .beginDatetime(Instant.parse("2023-11-08T08:00:00.00Z"))
        .endDatetime(Instant.parse("2023-11-08T10:00:00.00Z"))
        .description("Another Prog1 course")
        .eventType(INTEGRATION)
        .plannerId(MANAGER_ID)
        .groups(List.of(createGroupIdentifier(group1()), createGroupIdentifier(group2())));
  }

  public static Event expectedCourseEventCreated() {
    return new Event()
        .type(COURSE)
        .beginDatetime(createEventCourse1().getBeginDatetime())
        .endDatetime(createEventCourse1().getEndDatetime())
        .planner(planner1())
        .course(course1())
        .description(createEventCourse1().getDescription())
        .course(null);
  }

  public static Event expectedIntegrationEventCreated() {
    return new Event()
        .type(INTEGRATION)
        .planner(planner1())
        .course(null)
        .description(createIntegrationEvent().getDescription())
        .beginDatetime(createIntegrationEvent().getBeginDatetime())
        .endDatetime(createIntegrationEvent().getEndDatetime());
  }

  public static AnnouncementAuthor author1() {
    return new AnnouncementAuthor()
        .id(MANAGER_ID)
        .firstName(manager1().getFirstName())
        .lastName(manager1().getLastName())
        .email(manager1().getEmail())
        .profilePicture(null);
  }

  public static Announcement announcementForAll() {
    return new Announcement()
        .id(ANNOUNCEMENT1_ID)
        .title("Fermeture du bureau")
        .content("Le bureau est fermé pour ce weekend")
        .author(author1())
        .scope(GLOBAL)
        .creationDatetime(Instant.parse("2022-12-20T08:00:00.00Z"));
  }

  public static Announcement announcementForTeacher() {
    return new Announcement()
        .id(ANNOUNCEMENT2_ID)
        .title("Congé autorisé")
        .content("A tous les enseignants, vous êtes disposés à prendre des congés")
        .author(author1())
        .creationDatetime(Instant.parse("2022-12-21T08:00:00.00Z"))
        .scope(Scope.TEACHER);
  }

  public static Announcement announcementEspeciallyForG1() {
    return new Announcement()
        .id(ANNOUNCEMENT3_ID)
        .title("Cours annulé pour G1")
        .content("Les cours des G1 sont annulés pour demain")
        .author(
            new AnnouncementAuthor()
                .id(TEACHER1_ID)
                .firstName(teacher1().getFirstName())
                .lastName(teacher1().getLastName())
                .email(teacher1().getEmail())
                .profilePicture(null))
        .creationDatetime(Instant.parse("2022-12-22T08:00:00.00Z"))
        .scope(STUDENT);
  }

  public static Announcement announcementForManager() {
    return new Announcement()
        .id(ANNOUNCEMENT4_ID)
        .author(author1())
        .scope(Scope.MANAGER)
        .content("Veuillez vérifier nos comptes")
        .title("Comptabilité")
        .creationDatetime(Instant.parse("2022-12-15T08:00:00.00Z"));
  }

  public static CreateAnnouncement createAnnouncementWithGroupTarget() {
    return new CreateAnnouncement()
        .scope(STUDENT)
        .title("Cours de PROG1")
        .authorId(MANAGER_ID)
        .content("Cours prévu pour la semaine prochaine")
        .targetGroupList(List.of(createGroupIdentifier(group1())));
  }

  public static CreateAnnouncement teacherCreateAnnouncementForManager() {
    return new CreateAnnouncement()
        .scope(Scope.MANAGER)
        .title("Vacances de Pâques")
        .content("Plus de cours à partir de la semaine prochaine")
        .authorId(TEACHER1_ID)
        .targetGroupList(List.of());
  }

  public static Announcement expectedAnnouncementCreated1() {
    return new Announcement()
        .scope(STUDENT)
        .title("Cours de PROG1")
        .content("Cours prévu pour la semaine prochaine")
        .author(author1());
  }

  public static Announcement expectedAnnouncementCreated2() {
    return new Announcement()
        .scope(GLOBAL)
        .title("Vacances de Pâques")
        .content("Plus de cours à partir de la semaine prochaine")
        .author(author1());
  }

  public static Promotion promotion21() {
    return new Promotion()
        .id(PROMOTION1_ID)
        .ref("PROM21")
        .name("Promotion 21-22")
        .creationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"))
        .groups(List.of(createGroupIdentifier(group1()), createGroupIdentifier(group2())));
  }

  public static Promotion promotion22() {
    return new Promotion()
        .id(PROMOTION2_ID)
        .ref("PROM22")
        .name("Promotion 22-23")
        .creationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"))
        .groups(List.of(createGroupIdentifier(group3()), createGroupIdentifier(group4())));
  }

  public static Promotion promotion23() {
    return new Promotion()
        .id(PROMOTION3_ID)
        .ref("PROM23")
        .name("Promotion 23-24")
        .creationDatetime(Instant.parse("2021-11-08T08:30:24.00Z"))
        .groups(List.of());
  }

  public static LetterStudent toLetterStudent(Student student) {
    return new LetterStudent()
        .id(student.getId())
        .email(student.getEmail())
        .ref(student.getRef())
        .lastName(student.getLastName())
        .firstName(student.getFirstName())
        .nic(student.getNic())
        .profilePicture(student.getProfilePicture());
  }

  public static Letter letter1() {
    return new Letter()
        .id(LETTER1_ID)
        .ref(LETTER1_REF)
        .student(toLetterStudent(student1()))
        .status(RECEIVED)
        .approvalDatetime(Instant.parse("2021-12-08T08:25:24.00Z"))
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .fileUrl(null)
        .description("Certificat de residence");
  }

  public static Letter letter2() {
    return new Letter()
        .id(LETTER2_ID)
        .ref(LETTER2_REF)
        .student(toLetterStudent(student1()))
        .status(PENDING)
        .approvalDatetime(null)
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .fileUrl(null)
        .description("Bordereau de versement");
  }

  public static Letter letter3() {
    return new Letter()
        .id(LETTER3_ID)
        .ref(LETTER3_REF)
        .student(toLetterStudent(student2()))
        .status(PENDING)
        .approvalDatetime(null)
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .fileUrl(null)
        .description("CV");
  }

  public static UpdatePromotionSGroup addGroupToPromotion3() {
    return new UpdatePromotionSGroup().type(ADD).groupIds(List.of(group5().getId()));
  }

  public static UpdatePromotionSGroup removeGroupToPromotion3() {
    return new UpdatePromotionSGroup().type(REMOVE).groupIds(List.of(group5().getId()));
  }

  public static CrupdatePromotion createPromotion4() {
    return new CrupdatePromotion().id("promotion4_id").name("Promotion 24").ref("PROM24");
  }

  public static boolean isBefore(String a, String b) {
    return a.compareTo(b) < 0;
  }

  public static boolean isBefore(int a, int b) {
    return a < b;
  }

  public static boolean isValidUUID(String candidate) {
    try {
      UUID.fromString(candidate);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static int anAvailableRandomPort() {
    try {
      return new ServerSocket(0).getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
