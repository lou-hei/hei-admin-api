package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static school.hei.haapi.integration.conf.TestUtils.GATE_KEEPER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.GATE_KEEPER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.GATE_KEEPER2_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_ID;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.crupdateGateKeeper1;
import static school.hei.haapi.integration.conf.TestUtils.gateKeeper1;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.uploadProfilePicture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.GateKeeper;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
public class GateKeeperIT extends FacadeITMockedThirdParties {
  @Autowired ObjectMapper objectMapper;
  @MockBean private EventBridgeClient eventBridgeClientMock;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
  }

  @Test
  @Disabled("Not implemented: gate keeper can modify picture")
  void gatekeeper_update_own_profile_picture() throws IOException, InterruptedException {
    HttpResponse<InputStream> response =
        uploadProfilePicture(localPort, GATE_KEEPER1_TOKEN, GATE_KEEPER1_ID, "gatekeeper");
    assertEquals(200, response.statusCode());

    GateKeeper gateKeeper = objectMapper.readValue(response.body(), GateKeeper.class);
    assertEquals(gateKeeper1().getRef(), gateKeeper.getRef());
  }

  @Test
  void read_student_ko() {
    ApiClient student1Client = anApiClient(GATE_KEEPER2_TOKEN);
    UsersApi api = new UsersApi(student1Client);

    assertThrowsForbiddenException(() -> api.getStudentById(STUDENT1_ID));
    assertThrowsForbiddenException(
        () -> api.getStudents(1, 20, null, null, null, null, null, null, null, null, null));
  }

  @Test
  void read_teacher_ko() {
    ApiClient teacher1Client = anApiClient(GATE_KEEPER1_TOKEN);
    UsersApi api = new UsersApi(teacher1Client);

    assertThrowsForbiddenException(() -> api.getTeacherById(TEACHER1_ID));
    assertThrowsForbiddenException(() -> api.getTeachers(1, 20, null, null, null, null, null));
  }

  @Test
  void gate_keeper_access_to_its_own_account_ok() throws ApiException {
    UsersApi api = new UsersApi(anApiClient(GATE_KEEPER1_TOKEN));
    GateKeeper gateKeeperById1 = api.getGateKeeperById(GATE_KEEPER1_ID);
    assertEquals(gateKeeper1(), gateKeeperById1);
  }

  @Test
  void manager_manipulate_gate_keeper_ok() throws ApiException {
    UsersApi api = new UsersApi(anApiClient(MANAGER1_TOKEN));
    String modifiedFirstName = "test";
    List<GateKeeper> gateKeepers =
        api.crupdateGateKeepers(List.of(crupdateGateKeeper1().firstName(modifiedFirstName)));
    assertEquals(modifiedFirstName, gateKeepers.getFirst().getFirstName());

    // Can turn the gateKeeper first name to normal stat
    List<GateKeeper> originalGateKeepers = api.crupdateGateKeepers(List.of(crupdateGateKeeper1()));
    assertEquals(gateKeeper1().getFirstName(), originalGateKeepers.getFirst().getFirstName());
  }

  @Test
  void gate_keeper_access_to_other_account_ko() {
    UsersApi api = new UsersApi(anApiClient(GATE_KEEPER2_TOKEN));
    assertThrowsForbiddenException(() -> api.getGateKeeperById(GATE_KEEPER1_ID));
  }
}
