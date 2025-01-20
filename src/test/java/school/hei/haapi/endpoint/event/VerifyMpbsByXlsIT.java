package school.hei.haapi.endpoint.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static school.hei.haapi.integration.conf.TestUtils.getMockedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.model.VerifyMpbsByXlsEvent;
import school.hei.haapi.endpoint.rest.controller.MpbsController;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.service.event.VerifyMpbsViaXlsService;

@Testcontainers
@AutoConfigureMockMvc
public class VerifyMpbsByXlsIT extends FacadeITMockedThirdParties {

  @Autowired VerifyMpbsViaXlsService verifyMpbsViaXlsService;
  @Autowired MpbsController mpbsController;

  @MockBean Mailer mailerMock;
  @MockBean EventProducer eventProducerMock;

  static VerifyMpbsByXlsEvent verifyMpbsByXlsEvent() {
    return VerifyMpbsByXlsEvent.builder()
        .file(getMockedFile("test-mpbs", ".xls"))
        .verificationInstant(Instant.parse("2021-11-08T08:25:24.00Z"))
        .build();
  }

  public static MultipartFile convertXlsFileToMultipartFile(File file) throws IOException {
    FileInputStream inputStream = new FileInputStream(file);

    return new MockMultipartFile("file", file.getName(), "application/vnd.ms-excel", inputStream);
  }

  @Test
  @Disabled
  void should_invoke_event_producer() throws IOException {
    mpbsController.verifyMpbs(convertXlsFileToMultipartFile(getMockedFile("test-mpbs", ".xls")));

    verify(eventProducerMock, times(1)).accept(any());
  }

  @Test
  @Disabled
  void should_invoke_mailer() {
    verifyMpbsViaXlsService.accept(verifyMpbsByXlsEvent());

    verify(mailerMock, times(1)).accept(any());
  }
}
