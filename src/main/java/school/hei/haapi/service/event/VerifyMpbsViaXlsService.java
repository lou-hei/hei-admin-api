package school.hei.haapi.service.event;

import static school.hei.haapi.service.utils.DataFormatterUtils.instantToCommonDate;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.VerifyMpbsByXlsEvent;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.service.MpbsVerificationService;

@AllArgsConstructor
@Service
@Slf4j
public class VerifyMpbsViaXlsService implements Consumer<VerifyMpbsByXlsEvent> {
  private final Mailer mailer;
  private final MpbsVerificationService mpbsVerificationService;

  @Override
  public void accept(VerifyMpbsByXlsEvent event) {
    try {
      List<Mpbs> mpbs = mpbsVerificationService.computeFromXls(event.getFile());
      sendEmail(event, mpbs.size());
    } catch (IOException | AddressException e) {
      throw new RuntimeException(e);
    }
  }

  private Context getMailContext(VerifyMpbsByXlsEvent event, Integer count) {
    Context initial = new Context();
    initial.setVariable("instant", instantToCommonDate(event.getVerificationInstant()));
    initial.setVariable("count", count);
    return initial;
  }

  private void sendEmail(VerifyMpbsByXlsEvent event, Integer count) throws AddressException {
    String htmlBody = htmlToString("verifyMpbsByXlsEmail", getMailContext(event, count));
    mailer.accept(
        new Email(
            new InternetAddress("contact@mail.hei.school"),
            List.of(),
            List.of(),
            "Verification MPBS",
            htmlBody,
            List.of()));
  }
}
