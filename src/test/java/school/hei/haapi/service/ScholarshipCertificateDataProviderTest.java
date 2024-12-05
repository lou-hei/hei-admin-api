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
    User student9 = userService.findById("student9_id");
    User student10 = userService.findById("student10_id");

    Instant customInstant = Instant.parse("2025-11-08T08:25:24.00Z");

    String academicYearStudent9 =
        scholarshipCertificateDataProvider.getAcademicYear(student9, customInstant);
    String academicYearStudent10 =
        scholarshipCertificateDataProvider.getAcademicYear(student10, customInstant);

    assertEquals("Deuxième", academicYearStudent9);
    assertEquals("Première", academicYearStudent10);
  }
}
