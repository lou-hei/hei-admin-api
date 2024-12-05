package school.hei.haapi.service.event;

import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static school.hei.haapi.service.utils.DataFormatterUtils.instantToCommonDate;
import static school.hei.haapi.service.utils.DataFormatterUtils.numberToReadable;
import static school.hei.haapi.service.utils.DataFormatterUtils.numberToWords;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.UnpaidFeesReminder;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.model.exception.ApiException;

@Service
@AllArgsConstructor
@Slf4j
public class UnpaidFeesReminderService implements Consumer<UnpaidFeesReminder> {
  private Mailer mailer;

  private static String formatName(UnpaidFeesReminder.UnpaidFeesUser student) {
    return student.lastName() + " " + student.firstName();
  }

  private Context getMailContext(UnpaidFeesReminder unpaidFee) {
    Context initial = new Context();

    initial.setVariable("fullName", formatName(unpaidFee.getUser()));
    initial.setVariable("comment", unpaidFee.getComment());
    initial.setVariable("dueDatetime", instantToCommonDate(unpaidFee.getDueDatetime()));
    initial.setVariable("remainingAmount", numberToReadable(unpaidFee.getRemainingAmount()));
    initial.setVariable("remainingAmWord", numberToWords(unpaidFee.getRemainingAmount()));
    return initial;
  }

  @Override
  public void accept(UnpaidFeesReminder unpaidFeesReminder) {
    String htmlBody = htmlToString("unpaidFeeReminderEmail", getMailContext(unpaidFeesReminder));
    try {
      log.info("Sending email to : {} ...", unpaidFeesReminder.getUser().email());
      mailer.accept(
          new Email(
              new InternetAddress(unpaidFeesReminder.getUser().email()),
              List.of(),
              List.of(),
              "Rappel - Paiement de mensualité",
              htmlBody,
              List.of()));
      log.info("Email sent...");
    } catch (AddressException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
