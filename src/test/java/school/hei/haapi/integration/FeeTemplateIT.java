package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.FEE_TEMPLATE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE_TEMPLATE1_NAME;
import static school.hei.haapi.integration.conf.TestUtils.FEE_TEMPLATE2_ID;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.createFeeTemplate2;
import static school.hei.haapi.integration.conf.TestUtils.feeTemplate1;
import static school.hei.haapi.integration.conf.TestUtils.feeTemplate2;
import static school.hei.haapi.integration.conf.TestUtils.feeTemplate3;
import static school.hei.haapi.integration.conf.TestUtils.setUpCasdoor;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;
import static school.hei.haapi.integration.conf.TestUtils.updateFeeTemplate1;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.FeeTemplate;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@Testcontainers
@AutoConfigureMockMvc
public class FeeTemplateIT extends FacadeITMockedThirdParties {

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
  void manager_get_fee_templates() throws ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(managerClient);

    List<FeeTemplate> actual = api.getFeeTemplates(null, null, null, 1, 10);

    assertEquals(4, actual.size());
    assertTrue(actual.contains(feeTemplate1()));
    assertTrue(actual.contains(feeTemplate3()));
  }

  @Test
  void student_get_fee_templates() throws ApiException {
    ApiClient studentClient = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(studentClient);

    List<FeeTemplate> actual = api.getFeeTemplates(null, null, null, 1, 10);

    assertEquals(4, actual.size());
    assertTrue(actual.contains(feeTemplate1()));
    assertTrue(actual.contains(feeTemplate3()));
  }

  @Test
  void manager_create_fee_templates() throws ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(managerClient);

    FeeTemplate createdFeeTemplate =
        api.crupdateFeeTemplate(FEE_TEMPLATE2_ID, createFeeTemplate2());

    assertEquals(feeTemplate2().getName(), createdFeeTemplate.getName());
    assertEquals(feeTemplate2().getAmount(), createdFeeTemplate.getAmount());
    assertEquals(feeTemplate2().getNumberOfPayments(), createdFeeTemplate.getNumberOfPayments());
  }

  @Test
  void manager_update_fee_template() throws ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(managerClient);

    FeeTemplate actual = api.crupdateFeeTemplate(FEE_TEMPLATE1_ID, updateFeeTemplate1());
    assertEquals(1000, actual.getAmount());
    assertEquals(1, actual.getNumberOfPayments());
    assertEquals(FEE_TEMPLATE1_ID, actual.getId());
    assertEquals(FEE_TEMPLATE1_NAME, actual.getName());
  }

  @Test
  void get_fee_template_by_id_existing() throws ApiException {
    ApiClient managerClient = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(managerClient);

    FeeTemplate actual = api.getFeeTemplateById(FEE_TEMPLATE1_ID);

    assertEquals(feeTemplate1(), actual);
  }

  @Test
  void student_get_fee_template_by_id_existing() throws ApiException {
    ApiClient managerClient = anApiClient(STUDENT1_TOKEN);
    PayingApi api = new PayingApi(managerClient);

    FeeTemplate actual = api.getFeeTemplateById(FEE_TEMPLATE1_ID);

    assertEquals(feeTemplate1(), actual);
  }
}
