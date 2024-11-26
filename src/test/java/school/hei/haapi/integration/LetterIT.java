package school.hei.haapi.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FileType.OTHER;
import static school.hei.haapi.endpoint.rest.model.LetterStatus.*;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.*;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.EventsApi;
import school.hei.haapi.endpoint.rest.api.FilesApi;
import school.hei.haapi.endpoint.rest.api.LettersApi;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.EventParticipant;
import school.hei.haapi.endpoint.rest.model.EventParticipantLetter;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.FileInfo;
import school.hei.haapi.endpoint.rest.model.Letter;
import school.hei.haapi.endpoint.rest.model.UpdateLettersStatus;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = LetterIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
public class LetterIT extends MockedThirdParties {
  @MockBean EventBridgeClient eventBridgeClientMock;
  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void manager_read_all_ok() throws Exception {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    assertThrowsForbiddenException(
        () -> api.getLetters(1, 15, null, null, null, null, null, null, null));
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    List<Letter> actual = api.getStudentsLetters(1, 15, null, null, null, null, null, null);
    assertTrue(actual.contains(letter1()));
    assertTrue(actual.contains(letter2()));
    assertTrue(actual.contains(letter3()));
    assertFalse(actual.contains(teacherLetter()));

    List<Letter> filteredByStudentRef =
        api.getStudentsLetters(1, 15, "STD21001", null, null, null, null, null);
    assertTrue(filteredByStudentRef.contains(letter1()));
    assertTrue(filteredByStudentRef.contains(letter2()));
    assertFalse(filteredByStudentRef.contains(letter3()));

    List<Letter> filteredByStudentName =
        api.getStudentsLetters(1, 15, null, null, null, "Ryan", null, null);
    assertTrue(filteredByStudentName.contains(letter1()));
    assertTrue(filteredByStudentName.contains(letter2()));
    assertFalse(filteredByStudentName.contains(letter3()));

    List<Letter> filteredByLetterRef =
        api.getStudentsLetters(1, 15, null, "letter1_ref", null, null, null, null);
    assertTrue(filteredByLetterRef.contains(letter1()));
    assertFalse(filteredByLetterRef.contains(letter2()));

    List<Letter> actual3 = api.getStudentsLetters(1, 15, null, null, PENDING, null, null, null);
    assertFalse(actual3.contains(letter1()));
    assertTrue(actual3.contains(letter2()));
    assertTrue(actual3.contains(letter3()));
  }

  @Test
  void manager_read_by_id() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    Letter actual = api.getLetterById(LETTER1_ID);
    assertEquals(letter1(), actual);
  }

  @Test
  void manager_read_students_letter() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    List<Letter> actual = api.getLettersByUserId(STUDENT1_ID, 1, 15, null);
    assertTrue(actual.contains(letter1()));
    assertTrue(actual.contains(letter2()));
    assertFalse(actual.contains(letter3()));

    List<Letter> actual2 = api.getLettersByUserId(STUDENT2_ID, 1, 15, null);
    assertFalse(actual2.contains(letter1()));
    assertFalse(actual2.contains(letter2()));
    assertTrue(actual2.contains(letter3()));

    List<Letter> actual3 = api.getLettersByUserId(STUDENT1_ID, 1, 15, PENDING);
    assertFalse(actual3.contains(letter1()));
    assertTrue(actual3.contains(letter2()));
    assertFalse(actual3.contains(letter3()));
  }

  @Test
  void manager_create_and_update_students_letter()
      throws IOException, InterruptedException, ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);
    PayingApi payingApi = new PayingApi(apiClient);
    FilesApi filesApi = new FilesApi(apiClient);

    HttpResponse<InputStream> toBeReceived =
        uploadLetter(
            LetterIT.ContextInitializer.SERVER_PORT,
            MANAGER1_TOKEN,
            STUDENT1_ID,
            "Certificat",
            "file",
            null,
            null,
            null);
    Letter createdLetter1 = objectMapper.readValue(toBeReceived.body(), Letter.class);
    assertEquals(createdLetter1.getDescription(), "Certificat");
    assertEquals(PENDING, createdLetter1.getStatus());

    HttpResponse<InputStream> toBeRejected =
        uploadLetter(
            LetterIT.ContextInitializer.SERVER_PORT,
            MANAGER1_TOKEN,
            STUDENT1_ID,
            "A rejeter",
            "file",
            null,
            null,
            null);

    Letter createdLetter2 = objectMapper.readValue(toBeRejected.body(), Letter.class);
    assertEquals(createdLetter2.getDescription(), "A rejeter");
    assertEquals(PENDING, createdLetter2.getStatus());

    List<Letter> updatedLetters =
        api.updateLettersStatus(
            List.of(
                new UpdateLettersStatus().id(createdLetter1.getId()).status(RECEIVED),
                new UpdateLettersStatus()
                    .id(createdLetter2.getId())
                    .status(REJECTED)
                    .reasonForRefusal("Mauvais format")));

    Letter updatedLetter1 = updatedLetters.getFirst();
    assertEquals(RECEIVED, updatedLetter1.getStatus());
    assertNotNull(updatedLetter1.getApprovalDatetime());
    assertEquals(createdLetter1.getId(), updatedLetter1.getId());
    assertEquals(createdLetter1.getFee(), null);

    Letter updatedLetter2 = updatedLetters.get(1);
    assertEquals(REJECTED, updatedLetter2.getStatus());
    assertNotNull(updatedLetter2.getApprovalDatetime());
    assertEquals(createdLetter2.getId(), updatedLetter2.getId());

    // Check if the file info is saved
    List<FileInfo> fileInfos = filesApi.getUserFiles(STUDENT1_ID, 1, 15, OTHER);
    assertEquals(2, fileInfos.size());

    // Test fee payment
    HttpResponse<InputStream> testFeePayment =
        uploadLetter(
            LetterIT.ContextInitializer.SERVER_PORT,
            MANAGER1_TOKEN,
            STUDENT1_ID,
            "Test fee",
            "file",
            "fee7_id",
            5000,
            null);

    Letter createdLetter3 = objectMapper.readValue(testFeePayment.body(), Letter.class);
    Letter feeLetterUpdated =
        api.updateLettersStatus(
                List.of(new UpdateLettersStatus().id(createdLetter3.getId()).status(RECEIVED)))
            .getFirst();

    Fee actualFee = payingApi.getStudentFeeById(STUDENT1_ID, "fee7_id");
    assertEquals(actualFee.getComment(), feeLetterUpdated.getFee().getComment());
    assertEquals(actualFee.getType(), feeLetterUpdated.getFee().getType());
    assertEquals(actualFee.getStatus(), PAID);

    List<Letter> testFilterByFeeId =
        api.getStudentsLetters(1, 15, null, null, null, null, "fee7_id", null);
    assertEquals(testFilterByFeeId.getFirst().getId(), feeLetterUpdated.getId());
    assertFalse(testFilterByFeeId.contains(updatedLetter1));
    assertFalse(testFilterByFeeId.contains(updatedLetter2));

    List<Letter> testFilterByIsLinked =
        api.getStudentsLetters(1, 15, null, null, null, null, null, true);
    assertEquals(testFilterByIsLinked.getFirst().getId(), feeLetterUpdated.getId());
    assertFalse(testFilterByFeeId.contains(updatedLetter1));
    assertFalse(testFilterByFeeId.contains(updatedLetter2));

    List<Letter> testFilterByIsNotLinked =
        api.getStudentsLetters(1, 15, null, null, null, null, null, false);
    assertTrue(testFilterByIsNotLinked.contains(letter1()));
    assertTrue(testFilterByIsNotLinked.contains(letter2()));
  }

  @Test
  void test_letter_linked_with_event_participant()
      throws IOException, InterruptedException, ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);

    HttpResponse<InputStream> testEvent =
        uploadLetter(
            LetterIT.ContextInitializer.SERVER_PORT,
            MANAGER1_TOKEN,
            STUDENT3_ID,
            "Test event 1",
            "file",
            null,
            null,
            EVENT_PARTICIPANT5_ID);

    Letter createdLetter4 = objectMapper.readValue(testEvent.body(), Letter.class);

    assertEquals("Test event 1", createdLetter4.getDescription());

    EventParticipantLetter expectedEventParticipantLetter =
        new EventParticipantLetter()
            .creationDatetime(createdLetter4.getCreationDatetime())
            .status(createdLetter4.getStatus())
            .ref(createdLetter4.getRef())
            .description(createdLetter4.getDescription());

    EventsApi eventsApi = new EventsApi(apiClient);

    List<EventParticipant> eventParticipants =
        eventsApi.getEventParticipants(EVENT2_ID, 1, 15, null);
    assertEquals(expectedEventParticipantLetter, eventParticipants.get(2).getLetter().getFirst());
  }

  @Test
  void student_read_self_ok() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    List<Letter> actual = api.getLettersByUserId(STUDENT1_ID, 1, 15, null);
    assertTrue(actual.contains(letter1()));
    assertTrue(actual.contains(letter2()));
    assertFalse(actual.contains(letter3()));
  }

  @Test
  void teacher_read_others_letter_ko() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    assertThrowsForbiddenException(() -> api.getLettersByUserId(STUDENT1_ID, 1, 15, null));
  }

  @Test
  void teacher_upload_letter_for_fee_ko() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    assertThrowsForbiddenException(
        () ->
            api.createLetter(
                TEACHER1_ID,
                "filename",
                "description",
                "feeId",
                null,
                null,
                getMockedFile("img", ".png")));
  }

  @Test
  void teacher_read_self_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    List<Letter> actual = api.getLettersByUserId(TEACHER1_ID, 1, 15, null);
    assertEquals(actual.size(), 1);
  }

  @Test
  void student_forbidden_endpoint() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    LettersApi api = new LettersApi(apiClient);

    assertThrowsForbiddenException(() -> api.getLettersByUserId(STUDENT2_ID, 1, 15, null));
    assertThrowsForbiddenException(
        () ->
            api.updateLettersStatus(
                List.of(new UpdateLettersStatus().id("test").status(RECEIVED))));
    // TODO: The test should pass
    // assertThrowsForbiddenException(() -> api.createLetter(STUDENT2_ID,"null","null",
    // getMockedFile("img", ".png")));
  }

  @Test
  void student_upload_own_letter_ok() throws ApiException, IOException, InterruptedException {
    HttpResponse<InputStream> response =
        uploadLetter(
            LetterIT.ContextInitializer.SERVER_PORT,
            STUDENT1_TOKEN,
            STUDENT1_ID,
            "Certificat",
            "file",
            null,
            null,
            null);
    Letter createdLetter = objectMapper.readValue(response.body(), Letter.class);
    assertEquals(createdLetter.getDescription(), "Certificat");
    assertEquals(PENDING, createdLetter.getStatus());
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, LetterIT.ContextInitializer.SERVER_PORT);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
