package school.hei.haapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.ENABLED;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.SUSPENDED;
import static school.hei.haapi.integration.MpbsIT.createableMpbsFromFeeIdWithStudent1;
import static school.hei.haapi.integration.StudentIT.someCreatableStudent;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE3_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE6_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.creatableFee1;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
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
  private String FEE7_ID = "fee7_id";

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
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
