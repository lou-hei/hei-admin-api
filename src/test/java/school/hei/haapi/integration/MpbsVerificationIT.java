package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.AIRTEL_MONEY;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.MVOLA;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.ORANGE_MONEY;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE2_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.model.MpbsVerification;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.model.exception.ApiException;
import school.hei.haapi.service.mobileMoney.MobileMoneyApi;

@Testcontainers
@AutoConfigureMockMvc
public class MpbsVerificationIT extends FacadeITMockedThirdParties {

  @MockBean(name = "OrangeApi")
  MobileMoneyApi orangeApiMock;

  @MockBean(name = "MvolaApi")
  MobileMoneyApi mvolaApiMock;

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
    setUpMobileMock(orangeApiMock);
    setUpMobileMock(mvolaApiMock);
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  private void setUpMobileMock(MobileMoneyApi mobileMoneyApi) {
    when(mobileMoneyApi.getByTransactionRef(eq(MVOLA), any()))
        .thenReturn(attemptTransactionTelma());
    when(mobileMoneyApi.getByTransactionRef(eq(ORANGE_MONEY), any()))
        .thenReturn(attemptTransactionOrange());
    when(mobileMoneyApi.getByTransactionRef(eq(AIRTEL_MONEY), any())).thenThrow(ApiException.class);
  }

  public TransactionDetails attemptTransactionTelma() {
    return TransactionDetails.builder()
        .pspTransactionRef("TELMA-ref")
        .pspTransactionAmount(300_000)
        .pspDatetimeTransactionCreation(Instant.parse("2021-11-08T08:25:24.00Z"))
        .build();
  }

  public TransactionDetails attemptTransactionOrange() {
    return TransactionDetails.builder()
        .pspTransactionRef("ORANGE-ref")
        .pspTransactionAmount(300_000)
        .pspDatetimeTransactionCreation(Instant.parse("2021-11-08T08:25:24.00Z"))
        .build();
  }

  @Test
  void student_read_own_mpbs_verifications_ok()
      throws school.hei.haapi.endpoint.rest.client.ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    List<MpbsVerification> actual = api.getMpbsVerifications(STUDENT1_ID, FEE1_ID);

    assertEquals(expected1MpbsVerification(), actual.getFirst());
    assertTrue(actual.contains(expected1MpbsVerification()));
  }

  @Test
  void manager_read_mpbs_verification_ok()
      throws school.hei.haapi.endpoint.rest.client.ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<MpbsVerification> actual = api.getMpbsVerifications(STUDENT1_ID, FEE1_ID);

    assertTrue(actual.contains(expected1MpbsVerification()));
  }

  @Test
  void student_read_other_mpbs_verifications_ko()
      throws school.hei.haapi.endpoint.rest.client.ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsForbiddenException(() -> api.getMpbsVerifications(STUDENT2_ID, FEE2_ID));
  }

  public MpbsVerification expected1MpbsVerification() {
    return new MpbsVerification()
        .studentId(STUDENT1_ID)
        .feeId(FEE1_ID)
        .pspId("psp3_id")
        .pspType(MVOLA)
        .comment("comment 1")
        .amountInPsp(8000)
        .amountOfFeeRemainingPayment(8000)
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .creationDatetimeOfMpbs(Instant.parse("2021-11-08T08:25:24.00Z"))
        .creationDatetimeOfPaymentInPsp(Instant.parse("2021-11-08T08:25:24.00Z"));
  }
}
