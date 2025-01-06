package school.hei.haapi.service.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DateUtils {
  public static String getRecoveryDate(String dueDateString) {

    dueDateString = normalizeDateString(dueDateString);

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    LocalDate dueDate = LocalDate.parse(dueDateString, inputFormatter);

    LocalDate recoveryDate = dueDate.plusDays(15);

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
    return recoveryDate.format(outputFormatter);
  }

  private static String normalizeDateString(String dateString) {
    return dateString.replaceAll("(?<=\\b)([1-9])(?=\\s[a-zA-Z])", "0$1");
  }

  // Returns the start and end dates of the current month if the parameters are null
  public Instant[] getDefaultMonthRange(Instant monthFrom, Instant monthTo) {
    LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
    LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

    monthFrom =
        Objects.requireNonNullElse(
            monthFrom, firstDayOfMonth.atStartOfDay(ZoneOffset.UTC).toInstant());
    monthTo =
        Objects.requireNonNullElse(
            monthTo, lastDayOfMonth.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant());

    return new Instant[] {monthFrom, monthTo};
  }

  public static Instant convertStringToInstant(String dateString) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    TemporalAccessor temporalAccessor = formatter.parse(dateString);
    return Instant.from(temporalAccessor);
  }
}
