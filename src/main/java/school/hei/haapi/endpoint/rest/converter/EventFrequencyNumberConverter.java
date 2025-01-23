package school.hei.haapi.endpoint.rest.converter;

import org.springframework.core.convert.converter.Converter;
import school.hei.haapi.model.EventFrequencyNumber;

public class EventFrequencyNumberConverter implements Converter<String, EventFrequencyNumber> {

  @Override
  public EventFrequencyNumber convert(String source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return new EventFrequencyNumber(Integer.parseInt(source));
  }
}
