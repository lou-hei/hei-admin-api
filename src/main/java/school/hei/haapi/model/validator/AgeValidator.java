package school.hei.haapi.model.validator;

import static java.time.LocalDate.now;
import static school.hei.haapi.endpoint.rest.security.AuthProvider.getPrincipal;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.ForbiddenException;

@Component
@AllArgsConstructor
public class AgeValidator implements Consumer<Integer> {

  @Override
  public void accept(Integer minimumAge) {
    User currentUser = getPrincipal().getUser();
    int userAge = now().getYear() - currentUser.getBirthDate().getYear();
    if (userAge >= minimumAge && currentUser.getNic() == null) {
      throw new ForbiddenException(
          "Please complete your information at the Administration to be able to get your"
              + " certificate.");
    }
  }
}
