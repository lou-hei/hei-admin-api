package school.hei.haapi.endpoint.rest.validator;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import school.hei.haapi.http.model.CreateEventFrequency;
import school.hei.haapi.model.exception.BadRequestException;

@Component
public class CreateEventFrequencyValidator implements Consumer<CreateEventFrequency> {
  @Override
  public void accept(CreateEventFrequency toCreateEventFrequency) {
    Set<String> violationMessages = new HashSet<>();
    String beginningHour = toCreateEventFrequency.getFrequencyBeginningHour();
    String endHour = toCreateEventFrequency.getFrequencyEndingHour();

    if (isCreatable(toCreateEventFrequency)) {
      if (Objects.isNull(toCreateEventFrequency.getFrequencyScopeDay())) {
        violationMessages.add("Frequency cannot be created without selected day");
      }
      if (Objects.isNull(toCreateEventFrequency.getEventFrequencyNumber())) {
        violationMessages.add("Frequency cannot be created without number of");
      }
      if (Objects.isNull(beginningHour)) {
        violationMessages.add("Frequency cannot be created without beginning hour");
      }
      if (Objects.isNull(endHour)) {
        violationMessages.add("Frequency cannot be created without ending hour");
      }
      if (!Objects.isNull(beginningHour)
          && !beginningHour.isEmpty()
          && !isHourValid(beginningHour)) {
        violationMessages.add("Hour must be of format HH:MM");
      }
      if (!Objects.isNull(endHour) && !endHour.isEmpty() && !isHourValid(endHour)) {
        violationMessages.add("Hour must be of format HH:MM");
      }
      if (!violationMessages.isEmpty()) {
        String formattedViolationMessages =
            violationMessages.stream().map(String::toString).collect(Collectors.joining(". \n"));
        throw new BadRequestException(formattedViolationMessages);
      }
    }
  }

  private boolean isHourValid(String hour) {
    String regex = "^(?:[01]\\d|2[0-3]):[0-5]\\d$";
    return Pattern.matches(regex, hour);
  }

  private boolean isCreatable(CreateEventFrequency createEventFrequency) {
    return !Objects.isNull(createEventFrequency.getFrequencyBeginningHour())
        || !Objects.isNull(createEventFrequency.getFrequencyEndingHour())
        || !Objects.isNull(createEventFrequency.getFrequencyScopeDay())
        || !Objects.isNull(createEventFrequency.getEventFrequencyNumber());
  }
}
