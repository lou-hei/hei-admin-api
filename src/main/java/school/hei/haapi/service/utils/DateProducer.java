package school.hei.haapi.service.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.FrequencyScopeDay;

@Component
public class DateProducer {

  public List<LocalDate> apply(FrequencyScopeDay dayOfWeek, int goalDays) {
    List<LocalDate> days = new ArrayList<>();
    DayOfWeek day = DayOfWeek.valueOf(dayOfWeek.getValue());
    LocalDate now = LocalDate.now();
    int year = now.getYear();
    int month = now.getMonthValue();
    LocalDate date = LocalDate.of(year, month, 1);

    while (date.getDayOfWeek() != day) {
      date = date.plusDays(1);
    }

    while (days.size() < goalDays) {
      days.add(date);
      date = date.plusWeeks(1);

      if (date.getMonthValue() != month) {
        month = date.getMonthValue();
        year = date.getYear();
        date = LocalDate.of(year, month, 1);

        while (date.getDayOfWeek() != day) {
          date = date.plusDays(1);
        }
      }
    }
    return days;
  }
}
