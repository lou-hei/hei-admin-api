package school.hei.haapi.service.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

public class InstantUtils {
  private static final String UTC0 = "UTC+0";

  public static Instant now() {
    return LocalDateTime.now().atZone(ZoneId.of("UTC+3")).toInstant();
  }

  public static Instant getCurrentMondayOfTheWeek() {
    return LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay()
        .atZone(ZoneId.of(UTC0))
        .toInstant();
  }

  public static Instant getCurrentSaturdayOfTheWeek() {
    return LocalDate.now()
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .atStartOfDay()
        .atZone(ZoneId.of(UTC0))
        .toInstant();
  }

  public static Instant getToDay() {
    LocalDate now = LocalDate.now();
    return now.atStartOfDay(ZoneId.of("UTC")).plusHours(8).toInstant();
  }

  public static Instant getYesterday() {
    LocalDate now = LocalDate.now();
    return now.minusDays(1).atStartOfDay(ZoneId.of("UTC")).plusHours(8).toInstant();
  }

  public static Instant getFirstDayOfActualMonth() {
    return LocalDate.now()
        .with(TemporalAdjusters.firstDayOfMonth())
        .atStartOfDay(ZoneId.of("UTC+3"))
        .toInstant();
  }
}
