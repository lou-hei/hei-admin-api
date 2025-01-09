package school.hei.haapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.setUpS3Service;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.service.utils.ScholarshipCertificateDataProvider;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
class ScholarshipCertificateDataProviderTest extends FacadeITMockedThirdParties {
  public static final String STUDENT_9_ID = "student9_id";
  public static final String STUDENT_10_ID = "student10_id";
  @Autowired private ScholarshipCertificateDataProvider scholarshipCertificateDataProvider;
  @Autowired private UserService userService;
  @MockBean private EventBridgeClient eventBridgeClientMock;

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
    setUpEventBridge(eventBridgeClientMock);
  }

  @Test
  void get_academic_year_ok() {
    Instant customInstant1 = Instant.parse("2025-11-08T08:25:24.00Z");
    Instant customInstant2 = Instant.parse("2026-11-08T08:25:24.00Z");

    String promotionName1 = "Promotion 2025-2026";
    String promotionName2 = "Promotion 2024-2025";

    String academicYearStudent9 =
        scholarshipCertificateDataProvider.getAcademicYear(promotionName1, customInstant1);
    String academicYearStudent10 =
        scholarshipCertificateDataProvider.getAcademicYear(promotionName2, customInstant1);

    String academicYearStudent11 =
        scholarshipCertificateDataProvider.getAcademicYear(promotionName1, customInstant2);
    String academicYearStudent12 =
        scholarshipCertificateDataProvider.getAcademicYear(promotionName2, customInstant2);

    assertEquals("Première", academicYearStudent9);
    assertEquals("Deuxième", academicYearStudent10);

    assertEquals("Deuxième", academicYearStudent11);
    assertEquals("Troisième", academicYearStudent12);
  }
}
