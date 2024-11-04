package school.hei.haapi.service.event;

import static school.hei.haapi.endpoint.rest.model.LetterStatus.REJECTED;
import static school.hei.haapi.service.event.StudentsWithOverdueFeesReminderService.internetAddress;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.UpdateLetterEmail;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.service.utils.Base64Converter;
import school.hei.haapi.service.utils.ClassPathResourceResolver;

@AllArgsConstructor
@Service
@Slf4j
public class UpdateLetterEmailService implements Consumer<UpdateLetterEmail> {

  private final Mailer mailer;
  private final Base64Converter base64Converter;
  private final ClassPathResourceResolver classPathResourceResolver;

  private Context getMailContext(UpdateLetterEmail letter) {
    Context initial = new Context();
    Resource emailSignatureImage = classPathResourceResolver.apply("HEI_signature", ".png");

    initial.setVariable("emailSignature", base64Converter.apply(emailSignatureImage));
    initial.setVariable("description", letter.getDescription());
    initial.setVariable("ref", letter.getRef());
    initial.setVariable("reason", letter.getReason());
    return initial;
  }

  @Override
  public void accept(UpdateLetterEmail updateLetterEmail) {
    String htmlBody =
        htmlToString(
            updateLetterEmail.getStatus() == REJECTED ? "rejectLetterEmail" : "approveLetterEmail",
            getMailContext(updateLetterEmail));
    mailer.accept(
        new Email(
            internetAddress(updateLetterEmail.getEmail()),
            List.of(),
            List.of(),
            "HEI - Boîte aux lettres",
            htmlBody,
            List.of()));
    log.info("Email sent...");
  }
}
