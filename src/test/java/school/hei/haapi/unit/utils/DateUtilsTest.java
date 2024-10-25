package school.hei.haapi.unit.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.service.utils.DateUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DateUtilsTest.ContextInitializer.class)
@AutoConfigureMockMvc
public class DateUtilsTest extends MockedThirdParties {
  @Autowired private DateUtils dateUtils;
  @MockBean private EventBridgeClient eventBridgeClientMock;

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
  }

  @Test
  void get_recovery_date_ok() {
    String dueDateString1 = "31 mai 2024";
    String dueDateString2 = "9 mai 2024";
    String recoveryDate1 = dateUtils.getRecoveryDate(dueDateString1);
    String recoveryDate2 = dateUtils.getRecoveryDate(dueDateString2);
    String expectedRecoveryDate1 = "15 juin 2024";
    String expectedRecoveryDate2 = "24 mai 2024";
    assertEquals(expectedRecoveryDate1, recoveryDate1);
    assertEquals(expectedRecoveryDate2, recoveryDate2);
  }

  @Test
  public void get_default_month_range_ok() {
    LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
    LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

    Instant[] defaultRange = dateUtils.getDefaultMonthRange(null, null);

    assertEquals(firstDayOfMonth.atStartOfDay(ZoneOffset.UTC).toInstant(), defaultRange[0]);
    assertEquals(
        lastDayOfMonth.atTime(23, 59, 59, 999999999).atZone(ZoneOffset.UTC).toInstant(),
        defaultRange[1]);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
