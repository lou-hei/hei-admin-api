package school.hei.haapi.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.MpbsIT.createableMpbsFromFeeIdForStudent;
import static school.hei.haapi.integration.conf.TestUtils.FEE4_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE5_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.creatableFee1;
import static school.hei.haapi.integration.conf.TestUtils.setUpCasdoor;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.model.User.Sex.F;
import static school.hei.haapi.model.User.Sex.M;
import static school.hei.haapi.model.User.Status.ENABLED;
import static school.hei.haapi.model.User.Status.SUSPENDED;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.Mpbs;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.UserRepository;
import school.hei.haapi.service.FeeService;
import school.hei.haapi.service.MpbsService;
import school.hei.haapi.service.PaymentService;
import school.hei.haapi.service.UserService;
import school.hei.haapi.service.event.CheckSuspendedStudentsStatusService;
import school.hei.haapi.service.event.SuspendStudentsWithOverdueFeesService;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CheckStudentsStatusTest.ContextInitializer.class)
@AutoConfigureMockMvc
@Transactional
public class CheckStudentsStatusTest extends MockedThirdParties {
  @Autowired private CheckSuspendedStudentsStatusService checkSuspendedStudentsStatusService;
  @Autowired private SuspendStudentsWithOverdueFeesService suspendStudentsWithOverdueFeesService;
  @Autowired private PaymentService paymentService;
  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private MpbsService mpbsService;
  @Autowired private FeeService feeService;
  @MockBean private EventBridgeClient eventBridgeClientMock;

  @BeforeEach
  void setUp() {
    setUpCasdoor(casdoorAuthServiceMock);
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
  }

  private static User student1() {
    User student1 = new User();
    student1.setId("student1_id");
    student1.setFirstName("Ryan");
    student1.setLastName("Andria");
    student1.setEmail("test+ryan@hei.school");
    student1.setRef("STD21001");
    student1.setStatus(ENABLED);
    student1.setSex(M);
    student1.setBirthDate(LocalDate.parse("2000-01-01"));
    student1.setEntranceDatetime(Instant.parse("2021-11-08T08:25:24.00Z"));
    student1.setPhone("0322411123");
    student1.setAddress("Adr 1");
    student1.setLatitude(-123.123);
    student1.setLongitude(123.0);
    student1.setHighSchoolOrigin("Lycée Andohalo");
    return student1;
  }

  private static User student2() {
    User student2 = new User();
    student2.setId("student2_id");
    student2.setFirstName("Two");
    student2.setLastName("Student");
    student2.setEmail("test+student2@hei.school");
    student2.setRef("STD21002");
    student2.setStatus(ENABLED);
    student2.setSex(F);
    student2.setBirthDate(LocalDate.parse("2000-01-02"));
    student2.setEntranceDatetime(Instant.parse("2021-11-09T08:26:24.00Z"));
    student2.setPhone("0322411124");
    student2.setAddress("Adr 2");
    student2.setLatitude(255.255);
    student2.setLongitude(-255.255);
    student2.setHighSchoolOrigin("Lycée Andohalo");
    return student2;
  }

  @Test
  @DirtiesContext
  void update_students_status_ok() {
    User student2 = student2();
    assertEquals(ENABLED, student2.getStatus());

    // here, we check if the enabled student has paid all their fees
    suspendStudentsWithOverdueFeesService.suspendStudentsWithUnpaidOrLateFee();
    User suspendedStudent2 = userService.findById(student2.getId());
    assertEquals(SUSPENDED, suspendedStudent2.getStatus());

    paymentService.computeRemainingAmount(FEE4_ID, 5000);
    paymentService.computeRemainingAmount(FEE5_ID, 5000);

    // here, we check if the suspended student has paid all their fees
    checkSuspendedStudentsStatusService.updateStatusBasedOnPayment();
    User suspendedStudentChecked = userService.findById(student2.getId());
    assertEquals(ENABLED, suspendedStudentChecked.getStatus());
  }

  @Test
  @DirtiesContext
  void pending_students_status_ok() throws ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    ApiClient studentClient = anApiClient(STUDENT2_TOKEN);
    PayingApi managerPayingApi = new PayingApi(managerClient);
    PayingApi studentPayingApi = new PayingApi(studentClient);

    // Student2 must enable
    assertEquals(ENABLED, userService.findById(STUDENT2_ID).getStatus());

    // Create student 2 fee
    Fee student2Fee =
        managerPayingApi.createStudentFees(STUDENT2_ID, List.of(creatableFee1())).getFirst();
    Mpbs pendingMpbs =
        studentPayingApi.crupdateMpbs(
            STUDENT2_ID,
            student2Fee.getId(),
            createableMpbsFromFeeIdForStudent(STUDENT2_ID, student2Fee.getId()));

    suspendStudentsWithOverdueFeesService.suspendStudentsWithUnpaidOrLateFee();

    // student2 have unpaid or late fee
    assertTrue(
        userService.getStudentsWithUnpaidOrLateFee().contains(userService.findById(STUDENT2_ID)));

    // student2 is still ENABLE, because of the pending Mbps
    assertEquals(1, mpbsService.countPendingOfStudent(STUDENT2_ID));
    assertEquals(ENABLED, userService.findById(STUDENT2_ID).getStatus());
  }

  @Test
  void get_all_students_with_unpaid_or_late_fee_ok() {
    List<User> studentsWithUnpaidOrLateFee = userRepository.getStudentsWithUnpaidOrLateFee();

    assertEquals(2, studentsWithUnpaidOrLateFee.size());
    assertTrue(studentsWithUnpaidOrLateFee.contains(student1()));
    assertTrue(studentsWithUnpaidOrLateFee.contains(student2()));
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
