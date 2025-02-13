package school.hei.haapi.model;

import lombok.Getter;
import school.hei.haapi.model.exception.BadRequestException;

public class PageFromOne {
  // TODO: move into a better place with BoundedPageSize and FrequencyNumber
  @Getter private final int value;

  public PageFromOne(int value) {
    if (value < 1) {
      throw new BadRequestException("page value must be >= 1");
    }
    this.value = value;
  }
}
