package school.hei.haapi.endpoint.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.model.SendLetterEmail;
import school.hei.haapi.endpoint.event.model.UpdateLetterEmail;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.service.event.SendLetterEmailService;
import school.hei.haapi.service.event.UpdateLetterEmailService;
import school.hei.haapi.service.utils.Base64Converter;
import school.hei.haapi.service.utils.ClassPathResourceResolver;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = LetterEventIT.ContextInitializer.class)
@AutoConfigureMockMvc
public class LetterEventIT extends MockedThirdParties {

  @Autowired UpdateLetterEmailService updateLetterEmailService;
  @Autowired SendLetterEmailService sendLetterEmailService;
  @MockBean Mailer mailerMock;
  @MockBean Base64Converter base64Converter;
  @MockBean ClassPathResourceResolver classPathResourceResolver;

  static SendLetterEmail send() {
    return SendLetterEmail.builder()
        .id("test_id")
        .studentRef("ref")
        .description("description")
        .studentEmail("email")
        .build();
  }

  static UpdateLetterEmail letter1() {
    return UpdateLetterEmail.builder()
        .id("letter1_id")
        .email("test+ryan@hei.school")
        .description("Certificat de residence")
        .ref("letter1_ref")
        .build();
  }

  @Test
  void should_invoke_event_producer_when_pinging_manager() {
    sendLetterEmailService.accept(send());

    verify(mailerMock, times(1)).accept(any());
  }

  @Test
  void should_invoke_event_producer_when_pinging_student() {
    updateLetterEmailService.accept(letter1());

    verify(mailerMock, times(1)).accept(any());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
