package school.hei.haapi.service.event;

import static school.hei.haapi.service.utils.DataFormatterUtils.instantToCommonDate;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.SendVerifyMpbsByXlsEventEmail;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;

@AllArgsConstructor
@Slf4j
@Service
public class SendVerifyMpbsByXlsEventEmailService
    implements Consumer<SendVerifyMpbsByXlsEventEmail> {

  private final Mailer mailer;

  @Override
  public void accept(SendVerifyMpbsByXlsEventEmail sendVerifyMpbsByXlsEventEmail) {
    try {
      sendEmail(sendVerifyMpbsByXlsEventEmail);
    } catch (AddressException e) {
      throw new RuntimeException(e);
    }
  }

  private Context getMailContext(SendVerifyMpbsByXlsEventEmail event) {
    Context initial = new Context();
    initial.setVariable("instant", instantToCommonDate(event.getVerificationInstant()));
    initial.setVariable("count", event.getCount());
    return initial;
  }

  private void sendEmail(SendVerifyMpbsByXlsEventEmail event) throws AddressException {
    String htmlBody = htmlToString("verifyMpbsByXlsEmail", getMailContext(event));
    log.info("Sending verification email...");
    mailer.accept(
        new Email(
            new InternetAddress("contact@mail.hei.school"),
            List.of(),
            List.of(),
            "Verification par fichier XLS",
            htmlBody,
            List.of()));
  }
}
