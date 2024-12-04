package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.coordinatesWithValues;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.integration.conf.TestUtils.uploadProfilePicture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.UsersApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Coordinates;
import school.hei.haapi.endpoint.rest.model.CrupdateManager;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.Manager;
import school.hei.haapi.endpoint.rest.model.Sex;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@Testcontainers
@AutoConfigureMockMvc
public class ManagerIT extends FacadeITMockedThirdParties {
  @Autowired ObjectMapper objectMapper;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  public static Manager manager1() {
    Manager manager = new Manager();
    manager.setId("manager1_id");
    manager.setFirstName("One");
    manager.setLastName("Manager");
    manager.setEmail("test+manager1@hei.school");
    manager.setRef("MGR21001");
    manager.setPhone("0322411127");
    manager.setStatus(EnableStatus.ENABLED);
    manager.setSex(Sex.M);
    manager.setBirthDate(LocalDate.parse("1890-01-01"));
    manager.setEntranceDatetime(Instant.parse("2021-09-08T08:25:29Z"));
    manager.setAddress("Adr 5");
    manager.setBirthPlace("");
    manager.setNic("");
    manager.setCoordinates(new Coordinates().longitude(55.555).latitude(-55.555));
    return manager;
  }

  public static CrupdateManager someUpdatableManager1() {
    CrupdateManager manager = new CrupdateManager();
    manager.setId("manager1_id");
    manager.setFirstName("One");
    manager.setLastName("Manager");
    manager.setEmail("test+manager1@hei.school");
    manager.setRef("MGR21001");
    manager.setPhone("0322411127");
    manager.setStatus(EnableStatus.ENABLED);
    manager.setSex(Sex.M);
    manager.setBirthDate(LocalDate.parse("1890-01-01"));
    manager.setEntranceDatetime(Instant.parse("2021-09-08T08:25:29Z"));
    manager.setAddress("Adr 5");
    manager.setBirthPlace("");
    manager.setNic("");
    return manager
        .address("Adr 999")
        .sex(Sex.F)
        .lastName("Other last")
        .firstName("Other first")
        .coordinates(coordinatesWithValues())
        .birthDate(LocalDate.parse("2000-01-03"));
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, manager1());
  }

  @Test
  @Disabled("dirty")
  void manager_update_own_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);
    Manager updated = api.updateManager(MANAGER_ID, someUpdatableManager1());
    Manager managers = api.getManagerById(MANAGER_ID);
    assertEquals(managers, updated);
  }

  @Test
  void manager_update_own_profile_picture() throws IOException, InterruptedException {
    HttpResponse<InputStream> response =
        uploadProfilePicture(localPort, MANAGER1_TOKEN, MANAGER_ID, "managers");

    Manager manager = objectMapper.readValue(response.body(), Manager.class);

    assertEquals(200, response.statusCode());
    assertEquals("MGR21001", manager.getRef());
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(STUDENT1_TOKEN);

    UsersApi api = new UsersApi(student1Client);
    assertThrowsForbiddenException(() -> api.getManagerById(MANAGER_ID));
    assertThrowsForbiddenException(() -> api.getManagers(1, 20, null, null));
  }

  @Test
  void teacher_read_ko() {
    ApiClient teacher1Client = anApiClient(TEACHER1_TOKEN);

    UsersApi api = new UsersApi(teacher1Client);
    assertThrowsForbiddenException(() -> api.getManagerById(MANAGER_ID));
    assertThrowsForbiddenException(() -> api.getManagers(1, 20, null, null));
  }

  @Test
  void manager_read_own_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);

    UsersApi api = new UsersApi(manager1Client);
    Manager actual = api.getManagerById(MANAGER_ID);

    assertEquals(manager1(), actual);
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);

    UsersApi api = new UsersApi(manager1Client);
    List<Manager> managers = api.getManagers(1, 20, null, null);

    assertEquals(4, managers.size());
    assertEquals(manager1(), managers.getFirst());
  }
}
