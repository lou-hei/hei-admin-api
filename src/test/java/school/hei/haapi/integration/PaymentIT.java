package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.integration.StudentIT.someCreatableStudent;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE3_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE4_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE5_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE6_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.PAYMENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.PAYMENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.PAYMENT4_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.creatableFee1;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CreatePayment;
import school.hei.haapi.endpoint.rest.model.CrupdateStudent;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.Payment;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
class PaymentIT extends FacadeITMockedThirdParties {
  @Autowired EntityManager entityManager;
  @MockBean private EventBridgeClient eventBridgeClientMock;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  /***
   * Get payment by id without jpa, avoiding FILTER isDeleted = true | false
   * @param paymentId
   * @return Payment data by id
   */
  private school.hei.haapi.model.Payment getPaymentByIdWithoutJpaFiltering(String paymentId) {
    try {
      Query q =
          entityManager.createNativeQuery(
              "SELECT * FROM \"payment\" where id = ?", school.hei.haapi.model.Payment.class);
      q.setParameter(1, paymentId);
      return (school.hei.haapi.model.Payment) q.getSingleResult();
    } catch (NullPointerException e) {
      throw new school.hei.haapi.model.exception.ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  static Payment payment1() {
    return new Payment()
        .id(PAYMENT1_ID)
        .feeId(FEE1_ID)
        .type(Payment.TypeEnum.CASH)
        .amount(2000)
        .comment("Comment")
        .creationDatetime(Instant.parse("2022-11-08T08:25:24.00Z"));
  }

  static Payment payment2() {
    return new Payment()
        .id(PAYMENT2_ID)
        .feeId(FEE1_ID)
        .type(Payment.TypeEnum.MOBILE_MONEY)
        .amount(3000)
        .comment(null)
        .creationDatetime(Instant.parse("2022-11-10T08:25:25.00Z"));
  }

  static Payment payment4() {
    return new Payment()
        .id(PAYMENT4_ID)
        .feeId(FEE4_ID)
        .type(Payment.TypeEnum.SCHOLARSHIP)
        .amount(5000)
        .comment(null)
        .creationDatetime(Instant.parse("2022-11-12T08:25:26.00Z"));
  }

  static CreatePayment paymentWithAfterNowCreationDatetime() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.CASH)
        .amount(2000)
        .comment("creation datetime upper than now")
        .creationDatetime(Instant.now().plusSeconds(60));
  }

  static CreatePayment paymentNoCreationDatetime() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.CASH)
        .amount(2000)
        .comment("non given creation datetime");
  }

  static CreatePayment createWithBankType() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.BANK_TRANSFER)
        .amount(2000)
        .comment("Comment")
        .creationDatetime(Instant.parse("2022-11-08T08:25:24.00Z"));
  }

  static CreatePayment creatablePayment1() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.CASH)
        .amount(2000)
        .comment("Comment")
        .creationDatetime(Instant.parse("2022-11-08T08:25:24.00Z"));
  }

  static CreatePayment creatablePaymentZ() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.CASH)
        .amount(5000)
        .comment("Comment")
        .creationDatetime(Instant.parse("2022-11-08T08:25:24.00Z"));
  }

  static CreatePayment creatablePayment2() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.MOBILE_MONEY)
        .creationDatetime(Instant.parse("2022-11-10T08:25:25.00Z"))
        .amount(6000)
        .comment("Comment");
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
    setUpEventBridge(eventBridgeClientMock);
  }

  @Test
  void student_read_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    List<Payment> actual = api.getStudentPayments(STUDENT1_ID, FEE1_ID, 1, 5);

    assertTrue(actual.contains(payment1()));
    assertTrue(actual.contains(payment2()));
  }

  @Test
  void monitor_read_own_followed_student_payment_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi api = new PayingApi(monitor1Client);

    List<Payment> actual = api.getStudentPayments(STUDENT1_ID, FEE1_ID, 1, 5);

    assertTrue(actual.contains(payment1()));
    assertTrue(actual.contains(payment2()));
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<Payment> actual = api.getStudentPayments(STUDENT1_ID, FEE1_ID, 1, 5);

    assertTrue(actual.contains(payment1()));
    assertTrue(actual.contains(payment2()));
  }

  @Test
  @Disabled("dirty")
  void manager_delete_payment_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    Payment deletedPayment = api.deleteStudentFeePaymentById(STUDENT1_ID, FEE1_ID, PAYMENT1_ID);
    assertEquals(payment1(), deletedPayment);
    Fee actualFee = api.getStudentFeeById(STUDENT1_ID, FEE1_ID);
    assertEquals(2000, actualFee.getRemainingAmount());

    List<Payment> payments = api.getStudentPayments(STUDENT1_ID, FEE1_ID, 1, 5);
    assertFalse(payments.contains(deletedPayment));

    // test: check if the payment is not deleted but has been flagged as deleted.
    school.hei.haapi.model.Payment actualPaymentData =
        getPaymentByIdWithoutJpaFiltering(PAYMENT1_ID);
    assertTrue(actualPaymentData.isDeleted());
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentPayments(STUDENT2_ID, FEE3_ID, null, null));
  }

  @Test
  void monitor_read_other_student_payment_ko() {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi api = new PayingApi(monitor1Client);

    assertThrowsForbiddenException(() -> api.getStudentPayments(STUDENT2_ID, FEE3_ID, null, null));
  }

  @Test
  void teacher_read_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentPayments(STUDENT2_ID, FEE3_ID, null, null));
  }

  @Test
  void manager_write_with_bank_type_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<Payment> actual =
        api.createStudentPayments(STUDENT2_ID, FEE5_ID, List.of(createWithBankType()));

    List<Payment> expected = api.getStudentPayments(STUDENT2_ID, FEE5_ID, 1, 5);
    assertTrue(expected.containsAll(actual));
  }

  @Test
  void manager_write_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<Payment> actual =
        api.createStudentPayments(STUDENT1_ID, FEE3_ID, List.of(creatablePayment1()));

    List<Payment> expected = api.getStudentPayments(STUDENT1_ID, FEE3_ID, 1, 5);
    assertTrue(expected.containsAll(actual));
  }

  @Test
  @Disabled("dirty")
  void student_is_now_enabled_after_paying_fee() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi payingApi = new PayingApi(manager1Client);
    UsersApi usersApi = new UsersApi(manager1Client);
    CrupdateStudent subject = someCreatableStudent();
    subject.setStatus(EnableStatus.ENABLED);

    // Assert before all that the actual student is SUSPENDED ...
    Student student = usersApi.createOrUpdateStudents(List.of(subject), null).getFirst();
    assertEquals(EnableStatus.ENABLED, student.getStatus());
    // Update inserted user
    subject.setId(student.getId());
    subject.setStatus(EnableStatus.SUSPENDED);
    Student actualSuspended = usersApi.createOrUpdateStudents(List.of(subject), null).getFirst();
    assertEquals(EnableStatus.SUSPENDED, actualSuspended.getStatus());

    String subjectId = student.getId();

    // ... create corresponding fee ...
    Fee createdFee = payingApi.createStudentFees(subjectId, List.of(creatableFee1())).getFirst();

    // ... after all assert that the status of student is ENABLED
    payingApi.createStudentPayments(subjectId, createdFee.getId(), List.of(creatablePaymentZ()));
    Fee actualFee = payingApi.getStudentFeeById(subjectId, createdFee.getId());
    Student actualStudent = usersApi.getStudentById(subjectId);

    assertEquals(PAID, actualFee.getStatus());
    assertEquals(EnableStatus.ENABLED, actualStudent.getStatus());
  }

  @Test
  void teacher_write_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentPayments(STUDENT1_ID, FEE1_ID, List.of()));
  }

  @Test
  void student_write_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentPayments(STUDENT1_ID, FEE1_ID, List.of()));
  }

  @Test
  void manager_write_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    List<Payment> expected = api.getStudentPayments(STUDENT1_ID, FEE3_ID, 1, 5);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Payment amount (8000)"
            + " exceeds fee remaining amount (5000)\"}",
        () ->
            api.createStudentPayments(
                STUDENT1_ID, FEE3_ID, List.of(creatablePayment1(), creatablePayment2())));

    List<Payment> actual = api.getStudentPayments(STUDENT1_ID, FEE3_ID, 1, 5);
    assertEquals(0, expected.size());
    assertEquals(expected, actual);
  }

  @Test
  void manager_write_with_some_bad_fields_ko() {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    CreatePayment toCreate1 = creatablePayment1().amount(null);
    CreatePayment toCreate2 = creatablePayment1().amount(-1);

    ApiException exception1 =
        assertThrows(
            ApiException.class,
            () -> api.createStudentPayments(STUDENT1_ID, FEE1_ID, List.of(toCreate1)));
    ApiException exception2 =
        assertThrows(
            ApiException.class,
            () -> api.createStudentPayments(STUDENT1_ID, FEE1_ID, List.of(toCreate2)));

    String exceptionMessage1 = exception1.getMessage();
    String exceptionMessage2 = exception2.getMessage();
    assertTrue(exceptionMessage1.contains("Amount is mandatory"));
    assertTrue(exceptionMessage2.contains("Amount must be positive"));
  }

  @Test
  void manager_write_with_non_given_creation_datetime_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Creation datetime is mandatory\"}",
        () ->
            api.createStudentPayments(STUDENT1_ID, FEE3_ID, List.of(paymentNoCreationDatetime())));
  }

  @Test
  void manager_write_with_creation_datetime_after_current_time_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    // cannot test instant now when creating payment
    assertThrows(
        ApiException.class,
        () ->
            api.createStudentPayments(
                STUDENT1_ID, FEE3_ID, List.of(paymentWithAfterNowCreationDatetime())));
  }

  @Test
  void manager_write_changes_expected() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    Fee fee = api.getStudentFeeById(STUDENT1_ID, FEE6_ID);

    List<Payment> actual =
        api.createStudentPayments(fee.getStudentId(), fee.getId(), List.of(creatablePayment1()));

    List<Payment> expected = api.getStudentPayments(fee.getStudentId(), fee.getId(), 1, 10);

    Fee actualFee3 = api.getStudentFeeById(fee.getStudentId(), fee.getId());
    assertNotEquals(fee, actualFee3);
    assertEquals(
        (fee.getRemainingAmount() - creatablePayment1().getAmount()),
        actualFee3.getRemainingAmount());

    assertEquals(expected, actual);
  }
}
