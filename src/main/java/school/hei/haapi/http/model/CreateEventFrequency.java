package school.hei.haapi.http.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import school.hei.haapi.endpoint.rest.model.FrequencyScopeDay;
import school.hei.haapi.model.EventFrequencyNumber;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CreateEventFrequency implements Serializable {
  private FrequencyScopeDay frequencyScopeDay;
  private EventFrequencyNumber eventFrequencyNumber;
  private String frequencyBeginningHour;
  private String frequencyEndingHour;
}
