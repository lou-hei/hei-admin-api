package school.hei.haapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.ENABLED;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.SUSPENDED;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.HARDWARE;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.TUITION;
import static school.hei.haapi.integration.MpbsIT.createableMpbsFromFeeIdWithStudent1;
import static school.hei.haapi.integration.StudentIT.someCreatableStudent;
import static school.hei.haapi.integration.conf.TestUtils.FEE3_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE6_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.creatableFee1;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.model.User.Sex.F;
import static school.hei.haapi.model.User.Sex.M;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.User;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PaymentServiceTest.ContextInitializer.class)
@AutoConfigureMockMvc
class PaymentServiceTest extends MockedThirdParties {
  private static final Logger log = LoggerFactory.getLogger(PaymentServiceTest.class);
  @Autowired private PaymentService subject;
  @Autowired private MpbsService mpbsService;
  @MockBean private EventBridgeClient eventBridgeClientMock;
  @Autowired private FeeService feeService;
  @Autowired private UserService userService;
  private String FEE7_ID = "fee7_id";

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, TestUtils.student1());
    setUpEventBridge(eventBridgeClientMock);
  }

  @Test
  void user_status_is_computed_after_paying_fee_by_mpbs() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi usersApi = new UsersApi(manager1Client);
    PayingApi payingApi = new PayingApi(manager1Client);

    var correspondingCreateableStudent = someCreatableStudent();
    var correspondingFee =
        payingApi.createStudentFees(STUDENT1_ID, List.of(creatableFee1())).getFirst();
    var correspondingMpbs =
        payingApi.createMpbs(
            STUDENT1_ID,
            correspondingFee.getId(),
            createableMpbsFromFeeIdWithStudent1(correspondingFee.getId()));
    var correspondingStudent =
        usersApi.createOrUpdateStudents(List.of(correspondingCreateableStudent), null).getFirst();

    assertEquals(ENABLED, correspondingStudent.getStatus());
    correspondingCreateableStudent.setId(correspondingStudent.getId());
    correspondingCreateableStudent.setStatus(SUSPENDED);

    var correspondingStudentAfterMakingSUSPENDED =
        usersApi.createOrUpdateStudents(List.of(correspondingCreateableStudent), null).getFirst();

    assertEquals(SUSPENDED, correspondingStudentAfterMakingSUSPENDED.getStatus());

    var domainMpbs = mpbsService.getByPspId(correspondingMpbs.getPspId());
    subject.savePaymentFromMpbs(domainMpbs, 5000);

    // here correspondingStudent has paid all their fees late (fee3_id, fee6_id, fee7_id and the
    // created
    // correspondingFee)
    subject.computeRemainingAmount(FEE3_ID, 5000);
    subject.computeRemainingAmount(FEE6_ID, 5000);
    subject.computeRemainingAmount(FEE7_ID, 5000);
    subject.computeRemainingAmount(correspondingFee.getId(), 5000);

    var actualStudent1 = usersApi.getStudentById(STUDENT1_ID);
    assertEquals(ENABLED, actualStudent1.getStatus());
  }

  @Test
  @DirtiesContext
  void compute_user_status_after_paying_fee_ok() {
    User userWithUnpaidFees = student2();
    User userWithoutUnpaidFees = student3();

    subject.computeUserStatusAfterPayingFee(userWithUnpaidFees);
    subject.computeUserStatusAfterPayingFee(userWithoutUnpaidFees);
    User updatedUserWithUnpaidFees = userService.findById(userWithUnpaidFees.getId());
    User updatedUserWithoutUnpaidFees = userService.findById(userWithoutUnpaidFees.getId());

    assertEquals(User.Status.SUSPENDED, updatedUserWithUnpaidFees.getStatus());
    assertEquals(User.Status.ENABLED, updatedUserWithoutUnpaidFees.getStatus());

    // here student2 has paid all their fees late
    subject.computeRemainingAmount(student2UnpaidFee1().getId(), 5000);
    subject.computeRemainingAmount(student2UnpaidFee2().getId(), 5000);
    subject.computeUserStatusAfterPayingFee(userWithUnpaidFees);
    User userPaidAllLateFees = userService.findById(userWithUnpaidFees.getId());

    assertEquals(User.Status.ENABLED, userPaidAllLateFees.getStatus());
  }

  public static Fee student1UnpaidFee1() {
    Fee associatedFee = new Fee();
    associatedFee.setId("fee3_id");
    associatedFee.setStudent(student1());
    associatedFee.setType(TUITION);
    associatedFee.setComment("Comment");
    associatedFee.setRemainingAmount(5000);
    associatedFee.setTotalAmount(5000);
    associatedFee.setStatus(LATE);
    associatedFee.setCreationDatetime(Instant.parse("2022-12-08T08:25:24.00Z"));
    associatedFee.setDueDatetime(Instant.parse("2023-02-08T08:30:24.00Z"));
    associatedFee.setUpdatedAt(Instant.parse("2021-12-09T08:25:24.00Z"));
    return associatedFee;
  }

  public static Fee student2UnpaidFee1() {
    Fee associatedFee = new Fee();
    associatedFee.setId("fee4_id");
    associatedFee.setStudent(student2());
    associatedFee.setType(TUITION);
    associatedFee.setComment("Comment");
    associatedFee.setRemainingAmount(5000);
    associatedFee.setTotalAmount(5000);
    associatedFee.setStatus(LATE);
    associatedFee.setCreationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
    associatedFee.setDueDatetime(Instant.parse("2023-02-08T08:30:24.00Z"));
    associatedFee.setUpdatedAt(Instant.parse("2021-12-09T08:25:25.00Z"));
    return associatedFee;
  }

  public static Fee student2UnpaidFee2() {
    Fee associatedFee = new Fee();
    associatedFee.setId("fee5_id");
    associatedFee.setStudent(student2());
    associatedFee.setType(HARDWARE);
    associatedFee.setComment("Comment");
    associatedFee.setRemainingAmount(5000);
    associatedFee.setTotalAmount(5000);
    associatedFee.setStatus(LATE);
    associatedFee.setCreationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
    associatedFee.setDueDatetime(Instant.parse("2023-02-08T08:30:24.00Z"));
    associatedFee.setUpdatedAt(Instant.parse("2021-12-08T08:25:25.00Z"));
    return associatedFee;
  }

  public static User student1() {
    User student1 = new User();
    student1.setId("student1_id");
    student1.setFirstName("Ryan");
    student1.setLastName("Andria");
    student1.setEmail("test+ryan@hei.school");
    student1.setRef("STD21001");
    student1.setStatus(User.Status.ENABLED);
    student1.setSex(M);
    student1.setBirthDate(LocalDate.parse("2000-01-01"));
    student1.setEntranceDatetime(Instant.now());
    student1.setPhone("0123456789");
    student1.setAddress("Example Address");
    return student1;
  }

  public static User student2() {
    User student2 = new User();
    student2.setId("student2_id");
    student2.setFirstName("Two");
    student2.setLastName("Student");
    student2.setEmail("test+student2@hei.school");
    student2.setRef("STD21002");
    student2.setStatus(User.Status.ENABLED);
    student2.setSex(F);
    student2.setBirthDate(LocalDate.parse("2000-01-02"));
    student2.setEntranceDatetime(Instant.now());
    student2.setPhone("0322411124");
    student2.setAddress("Adr 2");
    return student2;
  }

  public static User student3() {
    User student2 = new User();
    student2.setId("student3_id");
    student2.setFirstName("Three");
    student2.setLastName("Student");
    student2.setEmail("test+student3@hei.school");
    student2.setRef("STD21003");
    student2.setStatus(User.Status.ENABLED);
    student2.setSex(F);
    student2.setBirthDate(LocalDate.parse("2000-01-02"));
    student2.setEntranceDatetime(Instant.now());
    student2.setPhone("0322411124");
    student2.setAddress("Adr 2");
    return student2;
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, PaymentServiceTest.ContextInitializer.SERVER_PORT);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
