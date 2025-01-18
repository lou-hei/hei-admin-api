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
    DayOfWeek targetDay = DayOfWeek.valueOf(dayOfWeek.getValue());
    LocalDate date = LocalDate.now();

    while (date.getDayOfWeek() != targetDay) {
      date = date.plusDays(1);
    }

    for (int i = 0; i < goalDays; i++) {
      days.add(date);
      date = date.plusWeeks(1);
    }

    return days;
  }
}
