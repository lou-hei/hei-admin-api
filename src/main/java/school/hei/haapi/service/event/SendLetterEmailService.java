package school.hei.haapi.service.event;

import static school.hei.haapi.service.event.StudentsWithOverdueFeesReminderService.internetAddress;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.SendLetterEmail;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.service.utils.Base64Converter;
import school.hei.haapi.service.utils.ClassPathResourceResolver;

@AllArgsConstructor
@Service
@Slf4j
public class SendLetterEmailService implements Consumer<SendLetterEmail> {

  private final Mailer mailer;
  private final Base64Converter base64Converter;
  private final ClassPathResourceResolver classPathResourceResolver;

  private Context getMailContext(SendLetterEmail letter) {
    Context initial = new Context();
    Resource emailSignatureImage = classPathResourceResolver.apply("HEI_signature", ".png");

    initial.setVariable("studentRef", letter.getStudentRef());
    initial.setVariable("description", letter.getDescription());
    initial.setVariable("emailSignature", base64Converter.apply(emailSignatureImage));
    return initial;
  }

  @Override
  public void accept(SendLetterEmail sendLetterEmail) {
    String htmlBody = htmlToString("sendLetterEmail", getMailContext(sendLetterEmail));
    mailer.accept(
        new Email(
            internetAddress(sendLetterEmail.getReceiver()),
            List.of(internetAddress(sendLetterEmail.getStudentEmail())),
            List.of(),
            "HEI - Boîte aux lettres - " + sendLetterEmail.getStudentRef(),
            htmlBody,
            List.of()));
    log.info("Email sent...");
  }
}
