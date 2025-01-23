package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PENDING;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.HARDWARE;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.REMEDIAL_COSTS;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.TUITION;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE2_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE3_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsApiException;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.creatableFee1;
import static school.hei.haapi.integration.conf.TestUtils.creatableStudentFee;
import static school.hei.haapi.integration.conf.TestUtils.createFeeForTest;
import static school.hei.haapi.integration.conf.TestUtils.fee1;
import static school.hei.haapi.integration.conf.TestUtils.fee2;
import static school.hei.haapi.integration.conf.TestUtils.fee3;
import static school.hei.haapi.integration.conf.TestUtils.fee4;
import static school.hei.haapi.integration.conf.TestUtils.requestFile;
import static school.hei.haapi.integration.conf.TestUtils.setUpCasdoor;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.consumer.EventConsumer;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CreateFee;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.FeesStatistics;
import school.hei.haapi.endpoint.rest.model.FeesWithStats;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.repository.FeeRepository;
import school.hei.haapi.repository.dao.FeeDao;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
class FeeIT extends FacadeITMockedThirdParties {
  @Autowired EventConsumer subject;
  @Autowired EntityManager entityManager;
  @Autowired FeeRepository feeRepository;
  @Autowired FeeDao feeDao;

  /***
   * Get fee by id without jpa, avoiding FILTER isDeleted = true | false
   * @param feeId
   * @return Fee data by id
   */
  private school.hei.haapi.model.Fee getFeeByIdWithoutJpaFiltering(String feeId) {
    try {
      Query q =
          entityManager.createNativeQuery(
              "SELECT * FROM \"fee\" where id = ?", school.hei.haapi.model.Fee.class);
      q.setParameter(1, feeId);
      return (school.hei.haapi.model.Fee) q.getSingleResult();
    } catch (NullPointerException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @BeforeEach
  void setUp() {
    setUpCasdoor(casdoorAuthServiceMock);
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void manager_delete_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    Fee createdFee = api.createStudentFees(STUDENT1_ID, List.of(createFeeForTest())).getFirst();

    Fee deletedFee = api.deleteStudentFeeById(createdFee.getId(), STUDENT1_ID);

    List<Fee> fees = api.getStudentFees(STUDENT1_ID, 1, 5, null);
    assertFalse(fees.contains(deletedFee));

    // test: check if the payment is not deleted but has been flagged as deleted.
    school.hei.haapi.model.Fee actualFeeData = getFeeByIdWithoutJpaFiltering(deletedFee.getId());
    assertTrue(actualFeeData.isDeleted());
  }

  @Test
  void student_read_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    Fee actualFee = api.getStudentFeeById(STUDENT1_ID, FEE1_ID);
    Fee test = api.getStudentFeeById(STUDENT1_ID, FEE3_ID);

    assertEquals(test, fee3());

    List<Fee> actual = api.getStudentFees(STUDENT1_ID, 1, 20, null);

    assertEquals(fee1(), actualFee);
    assertTrue(actual.contains(fee1()));
    assertTrue(actual.contains(fee2()));
    assertTrue(actual.contains(fee3()));
  }

  @Test
  void monitor_read_own_followed_student_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi api = new PayingApi(monitor1Client);

    Fee actualFee = api.getStudentFeeById(STUDENT1_ID, FEE1_ID);
    List<Fee> actual = api.getStudentFees(STUDENT1_ID, 1, 10, null);

    assertEquals(fee1(), actualFee);
    assertTrue(actual.contains(fee1()));
    assertTrue(actual.contains(fee2()));
    assertTrue(actual.contains(fee3()));
  }

  @Test
  void manager_read_fee_paid_by_mpbs() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    FeesWithStats actual =
        api.getFees(null, null, null, fee1().getCreationDatetime(), null, 1, 10, true, null);
    assertEquals(2, actual.getData().size());
  }

  @Test
  void read_fee_contains_student_first_name() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    FeesWithStats actual =
        api.getFees(null, null, null, fee1().getCreationDatetime(), null, 1, 10, true, null);
    assertNotNull(actual.getData().getFirst().getStudentFirstName());
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    Fee actualFee = api.getStudentFeeById(STUDENT1_ID, FEE1_ID);
    List<Fee> actualFees1 = api.getStudentFees(STUDENT1_ID, 1, 20, null);
    FeesWithStats actualFees2 =
        api.getFees(null, null, PAID, fee1().getCreationDatetime(), null, 1, 10, false, null);

    assertEquals(fee1(), actualFee);
    assertEquals(2, actualFees2.getData().size());
    assertTrue(actualFees1.contains(fee1()));
    assertTrue(actualFees1.contains(fee2()));
    assertTrue(actualFees1.contains(fee3()));
    assertTrue(actualFees2.getData().contains(fee1()));
    assertTrue(actualFees2.getData().contains(fee2()));

    FeesWithStats student2Fees =
        api.getFees(null, null, null, fee4().getDueDatetime(), null, 1, 5, false, "STD21002");
    assertEquals(student2Fees.getData().getFirst(), fee4());
    assertFalse(student2Fees.getData().contains(fee1()));
    assertFalse(student2Fees.getData().contains(fee2()));
    assertFalse(student2Fees.getData().contains(fee3()));
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFeeById(STUDENT2_ID, FEE2_ID));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFees(STUDENT2_ID, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getFees(null, null, null, null, null, 1, 10, false, null));
  }

  @Test
  void monitor_read_other_student_ko() {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi api = new PayingApi(monitor1Client);

    assertThrowsForbiddenException(() -> api.getStudentFeeById(STUDENT2_ID, FEE2_ID));
    assertThrowsForbiddenException(() -> api.getStudentFees(STUDENT2_ID, null, null, null));
    assertThrowsForbiddenException(
        () -> api.getFees(null, null, null, null, null, 1, 10, false, null));
  }

  @Test
  void teacher_read_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFeeById(STUDENT2_ID, FEE2_ID));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentFees(STUDENT2_ID, null, null, null));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getFees(null, null, null, null, null, 1, 10, false, null));
  }

  @Test
  void student_write_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    CreateFee createFee = creatableFee1();
    createFee.setType(REMEDIAL_COSTS);

    List<Fee> actualFee0 = api.createStudentFees(STUDENT1_ID, List.of(createFee));

    assertEquals(REMEDIAL_COSTS, actualFee0.getFirst().getType());

    List<Fee> actualFee1 = api.createStudentFees(STUDENT1_ID, List.of(creatableFee1()));

    assertEquals(TUITION, actualFee1.getFirst().getType());
  }

  @Test
  void student_write_other_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);
    CreateFee createFee = creatableFee1();
    createFee.setType(REMEDIAL_COSTS);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentFees(STUDENT2_ID, List.of(createFee)));
  }

  @Test
  void manager_write_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    Fee createdFee = api.createStudentFees(STUDENT1_ID, List.of(createFeeForTest())).getFirst();

    Fee updatedFee =
        createdFee.comment("M1 + M2 + M3").dueDatetime(Instant.parse("2021-11-09T10:10:10.00Z"));

    List<Fee> actualUpdated = api.updateStudentFees(STUDENT1_ID, List.of(updatedFee));

    assertEquals(1, actualUpdated.size());
    assertEquals(actualUpdated.getFirst().getComment(), updatedFee.getComment());
    assertEquals(actualUpdated.getFirst().getDueDatetime(), updatedFee.getDueDatetime());

    List<Fee> crupdatedStudentFees = api.crupdateStudentFees(List.of(creatableStudentFee()));

    List<Fee> student1Fees = api.getStudentFees(STUDENT1_ID, 1, 10, null);

    assertEquals(1, crupdatedStudentFees.size());
    assertTrue(student1Fees.contains(crupdatedStudentFees.getFirst()));
  }

  @Test
  void monitor_write_ko() {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi api = new PayingApi(monitor1Client);
    Fee feeUpdated =
        fee1().comment("nex comment").dueDatetime(Instant.parse("2021-11-09T10:10:10.00Z"));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.updateStudentFees(STUDENT1_ID, List.of(feeUpdated)));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentFees(STUDENT1_ID, List.of()));
  }

  @Test
  void teacher_write_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);
    Fee feeUpdated =
        fee1().comment("nex comment").dueDatetime(Instant.parse("2021-11-09T10:10:10.00Z"));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.updateStudentFees(STUDENT1_ID, List.of(feeUpdated)));
    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentFees(STUDENT1_ID, List.of()));
  }

  @Test
  void manager_write_with_some_bad_fields_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    CreateFee toCreate1 = creatableFee1().totalAmount(null);
    CreateFee toCreate2 = creatableFee1().totalAmount(-1);
    CreateFee toCreate3 = creatableFee1().dueDatetime(null);
    String wrongId = "wrong id";
    List<Fee> expected = api.getStudentFees(STUDENT1_ID, 1, 5, null);

    ApiException exception1 =
        assertThrows(
            ApiException.class, () -> api.createStudentFees(STUDENT1_ID, List.of(toCreate1)));
    ApiException exception2 =
        assertThrows(
            ApiException.class, () -> api.createStudentFees(STUDENT1_ID, List.of(toCreate2)));
    ApiException exception3 =
        assertThrows(
            ApiException.class, () -> api.createStudentFees(STUDENT1_ID, List.of(toCreate3)));
    ApiException exception4 =
        assertThrows(
            ApiException.class, () -> api.updateStudentFees(STUDENT1_ID, List.of(fee1().id(null))));
    ApiException exception6 =
        assertThrows(
            ApiException.class,
            () -> api.updateStudentFees(STUDENT1_ID, List.of(fee1().type(HARDWARE))));
    ApiException exception7 =
        assertThrows(
            ApiException.class,
            () -> api.updateStudentFees(STUDENT1_ID, List.of(fee1().remainingAmount(10))));
    ApiException exception9 =
        assertThrows(
            ApiException.class,
            () -> api.updateStudentFees(STUDENT1_ID, List.of(fee1().totalAmount(10))));
    ApiException exception10 =
        assertThrows(
            ApiException.class,
            () ->
                api.updateStudentFees(
                    STUDENT1_ID,
                    List.of(fee1().creationDatetime(Instant.parse("2021-11-09T10:10:10.00Z")))));
    ApiException exception11 =
        assertThrows(
            ApiException.class,
            () -> api.updateStudentFees(STUDENT1_ID, List.of(fee1().id(wrongId))));

    String exceptionMessage1 = exception1.getMessage();
    String exceptionMessage2 = exception2.getMessage();
    String exceptionMessage3 = exception3.getMessage();
    String exceptionMessage4 = exception4.getMessage();
    String exceptionMessage6 = exception6.getMessage();
    String exceptionMessage7 = exception7.getMessage();
    String exceptionMessage9 = exception9.getMessage();
    String exceptionMessage10 = exception10.getMessage();
    String exceptionMessage11 = exception11.getMessage();

    List<Fee> actual = api.getStudentFees(STUDENT1_ID, 1, 5, null);
    assertEquals(expected.size(), actual.size());

    assertTrue(expected.containsAll(actual));
    assertTrue(exceptionMessage1.contains("Total amount is mandatory"));
    assertTrue(exceptionMessage2.contains("Total amount must be positive"));
    assertTrue(exceptionMessage3.contains("Due datetime is mandatory"));

    assertTrue(exceptionMessage4.contains("Id is mandatory"));
    assertTrue(exceptionMessage6.contains("Can't modify Type"));
    assertTrue(exceptionMessage7.contains("Can't modify remainingAmount"));
    assertTrue(exceptionMessage9.contains("Can't modify total amount"));
    assertTrue(exceptionMessage10.contains("Can't modify CreationDatetime"));
    assertTrue(exceptionMessage11.contains("Fee with id " + wrongId + "does not exist"));
  }

  @Test
  void get_fees_by_criteria_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    FeesWithStats feeByMonth =
        api.getFees(
            null,
            null,
            null,
            Instant.parse("2021-12-01T00:00:00.00Z"),
            Instant.parse("2021-12-31T23:59:59.00Z"),
            1,
            10,
            false,
            null);
    assertEquals(9, feeByMonth.getData().size());
    assertTrue(feeByMonth.getData().contains(fee1()));
    assertTrue(feeByMonth.getData().contains(fee2()));
    assertTrue(feeByMonth.getData().contains(fee3()));
    assertTrue(feeByMonth.getData().contains(fee4()));

    FeesWithStats noFeeByMonth =
        api.getFees(
            null,
            null,
            null,
            Instant.parse("2021-10-01T00:00:00.00Z"),
            Instant.parse("2021-10-31T23:59:59.00Z"),
            1,
            10,
            false,
            null);
    assertEquals(0, noFeeByMonth.getData().size());

    FeesWithStats feeByStatusLateAndMonth =
        api.getFees(
            null,
            null,
            LATE,
            Instant.parse("2021-12-01T00:00:00.00Z"),
            Instant.parse("2021-12-31T23:59:59.00Z"),
            1,
            10,
            false,
            null);
    assertTrue(feeByStatusLateAndMonth.getData().contains(fee3()));
    assertTrue(feeByStatusLateAndMonth.getData().contains(fee4()));

    FeesWithStats feeByStatusPaidAndMonth =
        api.getFees(
            null,
            null,
            PAID,
            Instant.parse("2021-12-01T00:00:00.00Z"),
            Instant.parse("2021-12-31T23:59:59.00Z"),
            1,
            10,
            false,
            null);
    assertEquals(2, feeByStatusPaidAndMonth.getData().size());
    assertTrue(feeByStatusPaidAndMonth.getData().contains(fee1()));
    assertTrue(feeByStatusPaidAndMonth.getData().contains(fee2()));

    FeesWithStats feeByStatusLateAndMonthAndStudentRef =
        api.getFees(
            null,
            null,
            LATE,
            Instant.parse("2021-12-01T00:00:00.00Z"),
            Instant.parse("2021-12-31T23:59:59.00Z"),
            1,
            10,
            false,
            "STD21002");
    assertEquals(2, feeByStatusLateAndMonthAndStudentRef.getData().size());
    assertTrue(feeByStatusLateAndMonthAndStudentRef.getData().contains(fee4()));

    FeesWithStats feeIsMpbsByMonth =
        api.getFees(
            null,
            null,
            null,
            Instant.parse("2021-12-01T00:00:00.00Z"),
            Instant.parse("2021-12-31T23:59:59.00Z"),
            1,
            10,
            true,
            null);
    assertEquals(feeIsMpbsByMonth.getData().getLast(), fee1());
  }

  @Test
  void get_fees_statistics_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    FeesStatistics stats =
        api.getFeesStats(
            Instant.parse("2021-12-01T00:00:00.00Z"), Instant.parse("2021-12-31T00:00:00.00Z"));
    assertEquals(9, stats.getTotalFees());
    assertEquals(2, stats.getPaidFees());
    assertEquals(2, stats.getUnpaidFees());
  }

  @Test
  void generate_fees_list_as_xlsx_without_parameters_ok() throws IOException, InterruptedException {
    var response =
        requestFile(URI.create("http://localhost:" + localPort + "/fees/raw"), MANAGER1_TOKEN);

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(response.body());
    assertNotNull(response);
  }

  @Test
  void generate_fees_list_as_xlsx_with_parameters_ok() throws IOException, InterruptedException {
    var responseWithStatus =
        requestFile(
            URI.create("http://localhost:" + localPort + "/fees/raw?status=" + PENDING),
            MANAGER1_TOKEN);
    assertEquals(HttpStatus.OK.value(), responseWithStatus.statusCode());
    assertNotNull(responseWithStatus.body());
    assertNotNull(responseWithStatus);

    var responseWithDateStart =
        requestFile(
            URI.create(
                "http://localhost:"
                    + localPort
                    + "/fees/raw?from_due_datetime=2022-01-01T12:00:00.000Z"),
            MANAGER1_TOKEN);
    assertEquals(HttpStatus.OK.value(), responseWithDateStart.statusCode());
    assertNotNull(responseWithDateStart.body());
    assertNotNull(responseWithDateStart);

    var responseWithDateRange =
        requestFile(
            URI.create(
                "http://localhost:"
                    + localPort
                    + "/fees/raw?from_due_datetime=2022-01-01T12:00:00Z&to_due_datetime=2024-01-02T12:00:00Z"),
            MANAGER1_TOKEN);
    assertEquals(HttpStatus.OK.value(), responseWithDateRange.statusCode());
    assertNotNull(responseWithDateRange.body());
    assertNotNull(responseWithDateRange);
  }

  @Test
  void all_fee_without_status_and_dueDatetime_work() {
    var real_fees = feeRepository.findAll();
    var fees = feeDao.findAllByStatusAndDueDatetimeBetween(null, null, null);

    assertEquals(real_fees.size(), fees.size());
  }

  @Test
  void all_fee_by_status_and_dueDatetime_in_date_range_must_contain_some_fee() {
    var fees =
        feeDao.findAllByStatusAndDueDatetimeBetween(
            null,
            Instant.parse("2021-11-08T08:25:24.00Z"),
            Instant.parse("2022-12-08T08:25:24.00Z"));

    assertFalse(fees.isEmpty());
  }
}
