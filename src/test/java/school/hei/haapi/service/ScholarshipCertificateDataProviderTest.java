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
import school.hei.haapi.model.User;
import school.hei.haapi.service.utils.ScholarshipCertificateDataProvider;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
class ScholarshipCertificateDataProviderTest extends FacadeITMockedThirdParties {
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
    User student7 = userService.findById("student7_id");
    User student8 = userService.findById("student8_id");

    Instant expectedCurrentYear = Instant.parse("2023-11-08T08:25:24.00Z");

    String academicYearStudent7 =
        scholarshipCertificateDataProvider.getAcademicYear(student7, expectedCurrentYear);
    String academicYearStudent8 =
        scholarshipCertificateDataProvider.getAcademicYear(student8, expectedCurrentYear);

    assertEquals("Deuxième", academicYearStudent7);
    assertEquals("Première", academicYearStudent8);
  }
}
