package school.hei.haapi.service.utils;

import static java.util.stream.Collectors.toList;
import static school.hei.haapi.service.utils.InstantUtils.UTC0;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class DatetimeProducer implements BiFunction<List<LocalDate>, String, List<Instant>> {

  @Override
  public List<Instant> apply(List<LocalDate> localDates, String time) {
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    LocalTime localTime = LocalTime.parse(time, timeFormatter);

    return localDates.stream()
        .map(date -> date.atTime(localTime).atZone(UTC0).toInstant())
        .collect(toList());
  }
}
