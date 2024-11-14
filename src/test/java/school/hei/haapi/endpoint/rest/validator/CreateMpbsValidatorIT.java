package school.hei.haapi.endpoint.rest.validator;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.ORANGE_MONEY;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.model.CrupdateMpbs;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.model.exception.ApiException;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CreateMpbsValidatorIT.ContextInitializer.class)
@AutoConfigureMockMvc
public class CreateMpbsValidatorIT extends MockedThirdParties {
  @Autowired private CreateMpbsValidator subject;

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }

  @Test
  void assert_psp_id_is_given() {
    Exception exception =
        assertThrows(
            ApiException.class, () -> subject.accept("student1_id", "fee1_id", pspIdMissed()));
    String actualMessage = exception.getMessage();
    String expectedMessage = "Psp id is mandatory";

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void assert_psp_id_is_right() {
    Exception exception =
        assertThrows(
            ApiException.class, () -> subject.accept("student1_id", "fee1_id", pspIdWasWrong()));
    String actualMessage = exception.getMessage();
    String expectedMessage = "Psp id = must be 20 characters and must begin by MP";

    assertTrue(actualMessage.contains(expectedMessage));
  }

  private CrupdateMpbs pspIdMissed() {
    return new CrupdateMpbs().pspType(ORANGE_MONEY).feeId("fee1_id").studentId("student1_id");
  }

  private CrupdateMpbs pspIdWasWrong() {
    return new CrupdateMpbs()
        .pspType(ORANGE_MONEY)
        .feeId("fee1_id")
        .studentId("student1_id")
        .pspId("ieoaifnipoa");
  }
}
