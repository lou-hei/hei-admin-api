package school.hei.haapi.service.utils;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.springframework.stereotype.Component;

import static java.time.Month.DECEMBER;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;

@Component
public class SchoolYearUtils {
  private static List<Month> startOfSchoolYear = List.of(
      OCTOBER,
      NOVEMBER,
      DECEMBER
  );

  public static String getSchoolYear() {
    LocalDate now = LocalDate.now();
    LocalDate nextYear = now.plusYears(1L);
    LocalDate precedentYear = now.minusYears(1L);

    if (startOfSchoolYear.contains(now.getMonth())) {
      return now.getYear() + " - " + nextYear.getYear();
    }

    return precedentYear.getYear() + " - " + now.getYear();
  }
}
