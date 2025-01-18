package school.hei.haapi.service.utils;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;

@Component
public class DatetimeProducer implements BiFunction<List<LocalDate>, String, List<Instant>> {

    @Override
    public List<Instant> apply(List<LocalDate> localDates, String time) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime localTime = LocalTime.parse(time, timeFormatter);
        ZoneOffset zoneOffset = ZoneOffset.UTC;

        return localDates.stream()
                .map(date -> date.atTime(localTime)
                .toInstant(zoneOffset))
                .collect(toList());
    }
}
