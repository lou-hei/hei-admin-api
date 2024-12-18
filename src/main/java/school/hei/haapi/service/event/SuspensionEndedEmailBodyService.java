package school.hei.haapi.service.event;

import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.SuspensionEndedEmailBody;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.model.exception.ApiException;

@Service
@AllArgsConstructor
@Slf4j
public class SuspensionEndedEmailBodyService implements Consumer<SuspensionEndedEmailBody> {
  private final Mailer mailer;

  private InternetAddress getInternetAddressFromEmail(String email) {
    try {
      return new InternetAddress(email);
    } catch (AddressException e) {
      log.info("bad email address", e);
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private Context loadContext(SuspensionEndedEmailBody mailBodyContent) {
    Context initial = new Context();

    initial.setVariable("fullName", mailBodyContent.getMpbsAuthor());
    return initial;
  }

  @Override
  public void accept(SuspensionEndedEmailBody suspensionEndedEmailBody) {
    var body = htmlToString("suspensionEndedEmail", loadContext(suspensionEndedEmailBody));
    String mailTitle = "[ FIN DE SUSPENSION ]";
    InternetAddress to = getInternetAddressFromEmail(suspensionEndedEmailBody.getMpbsAuthorEmail());
    mailer.accept(new Email(to, List.of(), List.of(), mailTitle, body, List.of()));
    log.info("mail {} sent to {}", mailTitle, to);
  }
}
