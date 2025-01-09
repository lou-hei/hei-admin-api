package school.hei.haapi.service.event;

import static school.hei.haapi.service.event.StudentsWithOverdueFeesReminderService.internetAddress;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.SendReceiptZipToEmail;
import school.hei.haapi.file.FileZipper;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;

@Service
@AllArgsConstructor
@Slf4j
public class SendReceiptZipToEmailService implements Consumer<SendReceiptZipToEmail> {
  private final Mailer mailer;
  private FileZipper fileZipper;

  private Context getMailContext(SendReceiptZipToEmail sendReceiptZipToEmail) {
    Context initial = new Context();

    initial.setVariable("fileCount", sendReceiptZipToEmail.getFileToZip().size());
    return initial;
  }

  @Override
  public void accept(SendReceiptZipToEmail sendReceiptZipToEmail) {
    String htmlBody = htmlToString("feeReceiptEmail", getMailContext(sendReceiptZipToEmail));
    mailer.accept(
        new Email(
            internetAddress("contact@mail.hei.school"),
            List.of(internetAddress(sendReceiptZipToEmail.getEmailRecipient())),
            List.of(),
            "HEI - receipts of fee - start at " + sendReceiptZipToEmail.getStartRequest(),
            htmlBody,
            List.of(fileZipper.apply(sendReceiptZipToEmail.getFileToZip()))));
    log.info("Send email...");
  }
}
