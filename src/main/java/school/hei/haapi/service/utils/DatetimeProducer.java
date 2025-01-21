package school.hei.haapi.service.utils;

import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
    ZoneId zoneId = ZoneId.of("UTC+3");

    return localDates.stream()
        .map(date -> date.atTime(localTime).atZone(zoneId).toInstant())
        .collect(toList());
  }
}
