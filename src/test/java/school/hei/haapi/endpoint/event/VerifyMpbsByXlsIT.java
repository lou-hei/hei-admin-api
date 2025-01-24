package school.hei.haapi.endpoint.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.model.SendVerifyMpbsByXlsEventEmail;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.service.event.SendVerifyMpbsByXlsEventEmailService;

@Testcontainers
@AutoConfigureMockMvc
public class VerifyMpbsByXlsIT extends FacadeITMockedThirdParties {

  @Autowired SendVerifyMpbsByXlsEventEmailService service;

  @MockBean Mailer mailerMock;

  static SendVerifyMpbsByXlsEventEmail emailEvent() {
    return SendVerifyMpbsByXlsEventEmail.builder()
        .count(1)
        .verificationInstant(Instant.parse("2021-11-08T08:25:24.00Z"))
        .build();
  }

  @Test
  void should_invoke_mailer() {
    service.accept(emailEvent());

    verify(mailerMock, times(1)).accept(any());
  }
}
