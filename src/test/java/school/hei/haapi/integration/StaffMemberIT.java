package school.hei.haapi.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.StaffMemberIT.ContextInitializer.SERVER_PORT;
import static school.hei.haapi.integration.conf.TestUtils.ADMIN1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STAFF_MEMBER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STAFF_MEMBER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.integration.conf.TestUtils.teacher1;
import static school.hei.haapi.integration.conf.TestUtils.uploadProfilePicture;

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
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.StaffMember;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = StaffMemberIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
public class StaffMemberIT extends MockedThirdParties {

  @MockBean private EventBridgeClient eventBridgeClientMock;
  @Autowired ObjectMapper objectMapper;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpS3Service(fileService, teacher1());
  }

  @Test
  void admin_read_staff_members_ok() throws ApiException {
    ApiClient apiClient = anApiClient(ADMIN1_TOKEN);
    UsersApi api = new UsersApi(apiClient);

    List<StaffMember> actual = api.getStaffMembers(1, 15, null, null, null, null);
    assertEquals(3, actual.size());
  }

  @Test
  void admin_read_staff_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(ADMIN1_TOKEN);
    UsersApi api = new UsersApi(apiClient);

    assertNotNull(api.getStaffMemberById(STAFF_MEMBER1_ID));
  }

  @Test
  void staff_read_staff_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(STAFF_MEMBER1_TOKEN);
    UsersApi api = new UsersApi(apiClient);

    assertNotNull(api.getStaffMemberById(STAFF_MEMBER1_ID));
  }

  @Test
  void manager_read_ko() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(apiClient);

    assertThrowsForbiddenException(() -> api.getStaffMembers(1, 15, null, null, null, null));
    assertThrowsForbiddenException(() -> api.getStaffMemberById(STAFF_MEMBER1_ID));
  }

  @Test
  void staff_upload_profile_picture() throws IOException, InterruptedException {

    HttpResponse<InputStream> response =
        uploadProfilePicture(SERVER_PORT, STAFF_MEMBER1_TOKEN, STAFF_MEMBER1_ID, "staff_members");

    StaffMember staffMember = objectMapper.readValue(response.body(), StaffMember.class);

    assertEquals("STF21001", staffMember.getRef());
    assertEquals(200, response.statusCode());
  }

  @Test
  void admin_upload_profile_picture() throws IOException, InterruptedException {
    HttpResponse<InputStream> response =
        uploadProfilePicture(SERVER_PORT, ADMIN1_TOKEN, STAFF_MEMBER1_ID, "staff_members");

    StaffMember staffMember = objectMapper.readValue(response.body(), StaffMember.class);

    assertEquals("STF21001", staffMember.getRef());
    assertEquals(200, response.statusCode());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
