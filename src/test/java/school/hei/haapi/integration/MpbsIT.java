package school.hei.haapi.integration;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.TUITION;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.MVOLA;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.ORANGE_MONEY;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.PENDING;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.SUCCESS;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE2_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.getMockedFile;
import static school.hei.haapi.integration.conf.TestUtils.setUpCasdoor;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.CRLF;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.primitives.Bytes;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.mapper.MpbsMapper;
import school.hei.haapi.endpoint.rest.model.CreateFee;
import school.hei.haapi.endpoint.rest.model.CrupdateMpbs;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.FeeStatusEnum;
import school.hei.haapi.endpoint.rest.model.Mpbs;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.service.MpbsVerificationService;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
public class MpbsIT extends FacadeITMockedThirdParties {
  private static final Logger log = LoggerFactory.getLogger(MpbsIT.class);
  public static final String MPBS_FEE4_REF = "MP241210.0817.B36568";
  public static final String FEE8_ID = "fee8_id";
  @MockBean private EventBridgeClient eventBridgeClientMock;
  static final String FEE_TEST_ID = "test_id";
  @Autowired MpbsVerificationService verificationService;
  @Autowired MpbsMapper mpbsMapper;

  @BeforeEach
  public void setUp() {
    setUpCasdoor(casdoorAuthServiceMock);
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, student1());
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @Test
  void manager_read_student_mobile_money_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    Mpbs actual = api.getMpbs(STUDENT1_ID, FEE1_ID);

    assertEquals(expectedMpbs1(), actual);
  }

  @Test
  void student_read_own_mobile_money_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    Mpbs actual = api.getMpbs(STUDENT1_ID, FEE1_ID);

    assertEquals(expectedMpbs1(), actual);
  }

  @Test
  void monitor_read_own_followed_student_mobile_money_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi api = new PayingApi(monitor1Client);

    Mpbs actual = api.getMpbs(STUDENT1_ID, FEE1_ID);

    assertEquals(expectedMpbs1(), actual);
  }

  @Test
  void student_read_others_ko() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    assertThrowsForbiddenException(() -> api.getMpbs(STUDENT2_ID, FEE2_ID));
  }

  @Test
  void monitor_read_others_student_mobile_money_ko() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi api = new PayingApi(monitor1Client);

    assertThrowsForbiddenException(() -> api.getMpbs(STUDENT2_ID, FEE2_ID));
  }

  @Test
  @Disabled("dirty")
  void student_update_mobile_payment_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    Mpbs actual0 = api.getMpbs(STUDENT1_ID, FEE1_ID);
    assertEquals(expectedMpbs1(), actual0);

    Mpbs inUpdate = api.crupdateMpbs(STUDENT1_ID, FEE1_ID, updatableMpbs1());
    var updated = expectedMpbs1();
    updated.setPspId("MP240726.1541.D88426");
    updated.setPspType(ORANGE_MONEY);
    assertEquals(updated.getStudentId(), inUpdate.getStudentId());
    assertEquals(updated.getPspId(), inUpdate.getPspId());
    assertEquals(updated.getFeeId(), inUpdate.getFeeId());
    assertEquals(updated.getPspType(), inUpdate.getPspType());

    // Assert that one fee has only one mpbs
    Mpbs actual1 = api.getMpbs(STUDENT1_ID, FEE1_ID);
    actual1.setCreationDatetime(actual1.getCreationDatetime().truncatedTo(MINUTES));
    inUpdate.setCreationDatetime(inUpdate.getCreationDatetime().truncatedTo(MINUTES));
    assertEquals(actual1, inUpdate);

    // Assert that when we get fees it not throws error 500
    List<Fee> actualFee = api.getStudentFees(STUDENT1_ID, 1, 10, null);
    assertEquals(7, actualFee.size());
  }

  @Test
  void verify_mpbs_via_xls() throws ApiException, IOException, InterruptedException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(managerClient);

    // Check if the Mpbs exists
    Mpbs fee3BeforeVerification = api.getMpbs(STUDENT1_ID, FEE8_ID);

    assertEquals(PENDING, fee3BeforeVerification.getStatus());
    assertEquals(MPBS_FEE4_REF, fee3BeforeVerification.getPspId());

    // Upload xls file
    List<Mpbs> mpbsVerified =
        verificationService.computeFromXls(getMockedFile("test-mpbs", ".xls")).stream()
            .map(mpbsMapper::toRest)
            .toList();

    Mpbs actualMpbs = mpbsVerified.getFirst();

    assertEquals(SUCCESS, actualMpbs.getStatus());
    assertEquals(MPBS_FEE4_REF, actualMpbs.getPspId());

    // Check if the fee is paid
    Fee actualFee = api.getStudentFeeById(STUDENT1_ID, FEE8_ID);
    assertEquals(0, actualFee.getRemainingAmount());
    assertEquals(PAID, actualFee.getStatus());
  }

  public static HttpResponse<InputStream> uploadXls(Integer serverPort, String token)
      throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();

    String basePath = "http://localhost:" + serverPort;

    String boundary = "---------------------------" + System.currentTimeMillis();
    String contentTypeHeader = "multipart/form-data; boundary=" + boundary;

    File file = getMockedFile("test-mpbs", ".xls");

    String requestBodyPrefix =
        "--"
            + boundary
            + CRLF
            + "Content-Disposition: form-data; name=\"file_to_upload\"; filename=\""
            + file.getName()
            + "\""
            + CRLF
            + "Content-Type: application/vnd.ms-excel"
            + CRLF
            + CRLF;
    byte[] fileBytes = Files.readAllBytes(Paths.get(file.getPath()));
    String requestBodySuffix = CRLF + "--" + boundary + "--" + CRLF;

    byte[] requestBody =
        Bytes.concat(requestBodyPrefix.getBytes(), fileBytes, requestBodySuffix.getBytes());
    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromUri(URI.create(basePath + "/mpbs/verify"));
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

  public static List<Mpbs> convertResponseToStudentList(HttpResponse<InputStream> response)
      throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    try (InputStream inputStream = response.body()) {
      return objectMapper.readValue(inputStream, new TypeReference<List<Mpbs>>() {});
    }
  }

  @Test
  void student_create_mobile_payment_ok() throws ApiException {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi manager1Api = new PayingApi(manager1Client);

    Fee actualFee =
        manager1Api
            .createStudentFees(
                STUDENT1_ID,
                List.of(
                    new CreateFee()
                        .totalAmount(5000)
                        .dueDatetime(Instant.parse("2030-11-08T08:25:24.00Z"))
                        .type(TUITION)
                        .creationDatetime(Instant.now())
                        .comment("test")))
            .getFirst();
    assertEquals(actualFee.getStudentId(), STUDENT1_ID);

    Mpbs actual =
        api.crupdateMpbs(
            STUDENT1_ID, actualFee.getId(), createableMpbsFromFeeIdWithStudent1(actualFee.getId()));

    assertEquals(createableMpbs1().getStudentId(), actual.getStudentId());
    assertEquals(createableMpbs1().getPspId(), actual.getPspId());
    assertEquals(createableMpbs1().getPspType(), actual.getPspType());

    Fee updatedFee = api.getStudentFeeById(STUDENT1_ID, actualFee.getId());

    assertEquals(FeeStatusEnum.PENDING, updatedFee.getStatus());
  }

  public static CrupdateMpbs updatableMpbs1() {
    return new CrupdateMpbs()
        .id("mpbs1_id")
        .studentId(STUDENT1_ID)
        .feeId(FEE1_ID)
        .pspId("MP240726.1541.D88426")
        .pspType(ORANGE_MONEY);
  }

  public static Mpbs expectedMpbs1() {
    return new Mpbs()
        .id("mpbs1_id")
        .pspId("psp2_id")
        .studentId(STUDENT1_ID)
        .feeId(FEE1_ID)
        .pspType(MVOLA)
        .amount(8000)
        .successfullyVerifiedOn(Instant.parse("2021-11-08T08:25:24.00Z"))
        .creationDatetime(Instant.parse("2021-11-08T08:25:24.00Z"))
        .status(PENDING);
  }

  public static CrupdateMpbs createableMpbs1() {
    return new CrupdateMpbs()
        .studentId(STUDENT1_ID)
        .feeId(FEE2_ID)
        .pspType(ORANGE_MONEY)
        .pspId("MP240726.1541.D88425");
  }

  public static CrupdateMpbs createableMpbsFromFeeIdWithStudent1(String feeId) {
    return createableMpbsFromFeeIdForStudent(STUDENT1_ID, feeId);
  }

  public static CrupdateMpbs createableMpbsFromFeeIdForStudent(String studentId, String feeId) {
    return new CrupdateMpbs()
        .studentId(studentId)
        .feeId(feeId)
        .pspType(ORANGE_MONEY)
        .pspId("MP240726.1541.D88425");
  }
}
