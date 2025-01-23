package school.hei.haapi.service.event;

import static java.time.format.DateTimeFormatter.ofLocalizedDate;
import static java.time.format.FormatStyle.LONG;
import static java.util.Locale.FRENCH;
import static school.hei.haapi.service.utils.TemplateUtils.htmlToString;

import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import school.hei.haapi.endpoint.event.model.MissedEventEmail;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.User;
import school.hei.haapi.service.EventService;
import school.hei.haapi.service.UserService;

@Service
@AllArgsConstructor
public class MissedEventEmailService implements Consumer<MissedEventEmail> {
  private final UserService userService;
  private final EventService eventService;
  private final Mailer mailer;

  private final String MAIL_SUBJECT = "[HEI ADMIN] Notification d'absence";

  private String getStudentFullName(User user) {
    return user.getLastName() + " " + user.getFirstName();
  }

  private String formatDateFromInstant(Instant instant) {
    LocalDate date = LocalDate.from(instant);
    return date.format(ofLocalizedDate(LONG).withLocale(FRENCH));
  }

  private String formatHourFromInstant(Instant instant) {
    LocalTime time = LocalTime.from(instant);
    return time.format(DateTimeFormatter.ofPattern("HH'h'mm", FRENCH));
  }

  @Override
  public void accept(MissedEventEmail missedEventEmail) {
    String body = htmlToString("missedEventEmail", loadContext(missedEventEmail));
    mailer.accept(
        new Email(
            internetAddress(missedEventEmail.getStudentEmail()),
            List.of(),
            List.of(),
            MAIL_SUBJECT,
            body,
            List.of()));
  }

  private Context loadContext(MissedEventEmail missedEventEmail) {
    Context context = new Context();
    User student = userService.findById(missedEventEmail.getStudentId());
    Event event = eventService.findEventById(missedEventEmail.getEventId());

    context.setVariable("student_fullname", getStudentFullName(student));
    context.setVariable("course_date", formatDateFromInstant(event.getBeginDatetime()));
    context.setVariable("course_name", event.getCourse().getName());
    context.setVariable("course_start", formatHourFromInstant(event.getBeginDatetime()));
    context.setVariable("course_end", formatHourFromInstant(event.getEndDatetime()));

    return context;
  }

  @SneakyThrows
  public static InternetAddress internetAddress(String email) {
    return new InternetAddress(email);
  }
}
