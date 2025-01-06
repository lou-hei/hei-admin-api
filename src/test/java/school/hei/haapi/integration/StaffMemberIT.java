package school.hei.haapi.integration;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static school.hei.haapi.endpoint.rest.model.EnableStatus.ENABLED;
import static school.hei.haapi.endpoint.rest.model.Sex.F;
import static school.hei.haapi.integration.conf.TestUtils.ADMIN1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STAFF_MEMBER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STAFF_MEMBER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.coordinatesWithNullValues;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.integration.conf.TestUtils.teacher1;
import static school.hei.haapi.integration.conf.TestUtils.uploadProfilePicture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.StaffMember;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class StaffMemberIT extends FacadeITMockedThirdParties {

  @MockBean private EventBridgeClient eventBridgeClientMock;
  @Autowired ObjectMapper objectMapper;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
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
    log.info(actual.toString());
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
  void manager_read_ko() {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(apiClient);

    assertThrowsForbiddenException(() -> api.getStaffMembers(1, 15, null, null, null, null));
    assertThrowsForbiddenException(() -> api.getStaffMemberById(STAFF_MEMBER1_ID));
  }

  @Test
  void staff_upload_profile_picture() throws IOException, InterruptedException {

    HttpResponse<InputStream> response =
        uploadProfilePicture(localPort, STAFF_MEMBER1_TOKEN, STAFF_MEMBER1_ID, "staff_members");

    StaffMember staffMember = objectMapper.readValue(response.body(), StaffMember.class);

    assertEquals("STF21001", staffMember.getRef());
    assertEquals(200, response.statusCode());
  }

  @Test
  void admin_upload_profile_picture() throws IOException, InterruptedException {
    HttpResponse<InputStream> response =
        uploadProfilePicture(localPort, ADMIN1_TOKEN, STAFF_MEMBER1_ID, "staff_members");

    StaffMember staffMember = objectMapper.readValue(response.body(), StaffMember.class);

    assertEquals("STF21001", staffMember.getRef());
    assertEquals(200, response.statusCode());
  }

  @Test
  void admin_create_staff_member_ok() throws ApiException {
    ApiClient apiClient = anApiClient(ADMIN1_TOKEN);
    UsersApi api = new UsersApi(apiClient);

    StaffMember staffMember =
        new StaffMember()
            .address("test")
            .firstName("test")
            .lastName("test")
            .id("test_staff_id")
            .nic("test")
            .cnaps("cnaps")
            .ostie("ostie")
            .degree("degree")
            .function("function")
            .email("staff@gmail.com")
            .sex(F)
            .ref("staff_ref")
            .status(ENABLED)
            .entranceDatetime(Instant.now())
            .coordinates(coordinatesWithNullValues());

    List<StaffMember> actual = api.crupdateStaffMembers(List.of(staffMember));
    assertEquals(1, actual.size());
    assertEquals(staffMember.getDegree(), actual.getFirst().getDegree());
    assertEquals(staffMember.getFunction(), actual.getFirst().getFunction());

    List<StaffMember> after = api.getStaffMembers(1, 15, null, null, null, null);
    log.info(after.toString());
    assertEquals(4, after.size());
  }
}
