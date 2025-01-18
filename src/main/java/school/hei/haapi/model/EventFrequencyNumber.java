package school.hei.haapi.model;

import lombok.Getter;
import school.hei.haapi.model.exception.BadRequestException;

public class EventFrequencyNumber {
  @Getter private final int value;

  public EventFrequencyNumber(int value) {
    if (value < 1) {
      throw new BadRequestException("frequence value must be >= 1");
    }
    this.value = value;
  }
}
