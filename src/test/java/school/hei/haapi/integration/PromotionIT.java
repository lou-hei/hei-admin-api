package school.hei.haapi.integration;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.PROMOTION1_ID;
import static school.hei.haapi.integration.conf.TestUtils.PROMOTION3_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.addGroupToPromotion3;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.createGroupIdentifier;
import static school.hei.haapi.integration.conf.TestUtils.createPromotion4;
import static school.hei.haapi.integration.conf.TestUtils.group5;
import static school.hei.haapi.integration.conf.TestUtils.promotion21;
import static school.hei.haapi.integration.conf.TestUtils.promotion22;
import static school.hei.haapi.integration.conf.TestUtils.promotion23;
import static school.hei.haapi.integration.conf.TestUtils.removeGroupToPromotion3;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.PromotionsApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CrupdatePromotion;
import school.hei.haapi.endpoint.rest.model.Promotion;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@Testcontainers
@AutoConfigureMockMvc
public class PromotionIT extends FacadeITMockedThirdParties {

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void manager_generate_promotion_students_ok() throws IOException, InterruptedException {
    String STUDENTS_PROMOTION_PATH = "/promotions/" + PROMOTION1_ID + "/students";
    HttpClient httpClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> response =
        httpClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + STUDENTS_PROMOTION_PATH))
                .GET()
                .header("Authorization", "Bearer " + MANAGER1_TOKEN)
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertNotNull(response.body());
    assertNotNull(response);
  }

  @Test
  void manager_read_promotion_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    List<Promotion> allPromotions = api.getPromotions(1, 15, null, null, null);
    assertEquals(promotion21(), allPromotions.getFirst());
    assertTrue(allPromotions.contains(promotion21()));
    assertTrue(allPromotions.contains(promotion22()));
    assertTrue(allPromotions.contains(promotion23()));

    List<Promotion> promotionsFilteredByName =
        api.getPromotions(1, 15, "Promotion 2021-2022", null, null);
    assertTrue(promotionsFilteredByName.contains(promotion21()));
    assertFalse(promotionsFilteredByName.contains(promotion22()));
    assertFalse(promotionsFilteredByName.contains(promotion23()));

    List<Promotion> promotionsFilteredByRef = api.getPromotions(1, 15, null, "PROM22", null);
    assertTrue(promotionsFilteredByRef.contains(promotion22()));
    assertFalse(promotionsFilteredByRef.contains(promotion21()));
    assertFalse(promotionsFilteredByRef.contains(promotion23()));

    List<Promotion> promotionsFilteredByGroupRef = api.getPromotions(1, 15, null, null, "G1");
    assertTrue(promotionsFilteredByGroupRef.contains(promotion21()));
    assertFalse(promotionsFilteredByGroupRef.contains(promotion22()));
    assertFalse(promotionsFilteredByGroupRef.contains(promotion23()));
  }

  @Test
  void teacher_read_promotion_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    List<Promotion> allPromotions = api.getPromotions(1, 15, null, null, null);
    assertTrue(allPromotions.contains(promotion21()));
    assertTrue(allPromotions.contains(promotion22()));
    assertTrue(allPromotions.contains(promotion23()));
  }

  @Test
  void student_read_promotion_ok() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    List<Promotion> allPromotions = api.getPromotions(1, 15, null, null, null);
    assertTrue(allPromotions.contains(promotion21()));
    assertTrue(allPromotions.contains(promotion23()));
  }

  @Test
  void manager_create_or_update_promotion_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    Promotion promotion4Created = api.crupdatePromotion(createPromotion4());
    assertEquals(promotion4Created.getRef(), createPromotion4().getRef());
    assertEquals(promotion4Created.getName(), createPromotion4().getName());

    CrupdatePromotion updatePromotion4 =
        new CrupdatePromotion()
            .id(promotion4Created.getId())
            .ref(promotion4Created.getRef())
            .name("Nom de la promotion modifiée");

    Promotion promotion4Updated = api.crupdatePromotion(updatePromotion4);
    assertEquals(updatePromotion4.getId(), promotion4Updated.getId());
    assertEquals(updatePromotion4.getRef(), promotion4Updated.getRef());
    assertEquals(updatePromotion4.getName(), promotion4Updated.getName());
    assertEquals(promotion4Created.getCreationDatetime(), promotion4Updated.getCreationDatetime());
  }

  @Test
  void manager_read_promotion_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    Promotion actual = api.getPromotionById(PROMOTION1_ID);
    assertEquals(promotion21(), actual);
  }

  @Test
  void teacher_read_promotion_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    Promotion actual = api.getPromotionById(PROMOTION1_ID);
    assertEquals(promotion21(), actual);
  }

  @Test
  void student_read_promotion_by_id_ok() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    Promotion actual = api.getPromotionById(PROMOTION1_ID);
    assertEquals(promotion21(), actual);
  }

  @Test
  void manager_add_or_remove_groups_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    PromotionsApi api = new PromotionsApi(apiClient);

    Promotion promotion3withAddedGroup =
        api.updatePromotionGroups(PROMOTION3_ID, addGroupToPromotion3());
    assertEquals(1, promotion3withAddedGroup.getGroups().size());
    assertTrue(promotion3withAddedGroup.getGroups().contains(createGroupIdentifier(group5())));

    Promotion promotion3withRemovedGroup =
        api.updatePromotionGroups(PROMOTION3_ID, removeGroupToPromotion3());
    assertEquals(0, promotion3withRemovedGroup.getGroups().size());
    assertFalse(promotion3withRemovedGroup.getGroups().contains(createGroupIdentifier(group5())));
  }

  @Test
  void update_promotion_forbidden_ok() throws ApiException {
    ApiClient studentApiClient = anApiClient(STUDENT1_TOKEN);
    PromotionsApi studentCallAPi = new PromotionsApi(studentApiClient);

    ApiClient teacherApiClient = anApiClient(STUDENT1_TOKEN);
    PromotionsApi teacherCallAPi = new PromotionsApi(teacherApiClient);

    assertThrowsForbiddenException(() -> studentCallAPi.crupdatePromotion(createPromotion4()));
    assertThrowsForbiddenException(
        () -> studentCallAPi.updatePromotionGroups(PROMOTION3_ID, addGroupToPromotion3()));

    assertThrowsForbiddenException(() -> teacherCallAPi.crupdatePromotion(createPromotion4()));
    assertThrowsForbiddenException(
        () -> teacherCallAPi.updatePromotionGroups(PROMOTION3_ID, addGroupToPromotion3()));
  }
}
