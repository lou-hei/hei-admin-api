package school.hei.haapi.model.validator;

import static school.hei.haapi.endpoint.rest.security.AuthProvider.getPrincipal;
import static school.hei.haapi.model.User.Role.TEACHER;

import java.util.Objects;
import org.springframework.stereotype.Component;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.model.exception.ForbiddenException;

@Component
public class LetterValidator {

  public void accept(String feeId, String eventParticipantId, Integer amount) {
    User user = getPrincipal().getUser();

    if (user.getRole() == TEACHER && Objects.nonNull(eventParticipantId)) {
      throw new ForbiddenException();
    }
    if (user.getRole() == TEACHER && Objects.nonNull(feeId)) {
      throw new ForbiddenException();
    }
    if (Objects.nonNull(feeId) && Objects.nonNull(eventParticipantId)) {
      throw new BadRequestException("Cannot link letter with both fee and event participant");
    }

    if (Objects.nonNull(feeId) && Objects.isNull(amount)) {
      throw new BadRequestException("Cannot create a letter for a fee without a given amount");
    }
  }
}
