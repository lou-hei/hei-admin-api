package school.hei.haapi.model.validator;

import static java.time.LocalDate.now;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.ForbiddenException;

@Component
@AllArgsConstructor
public class ScholarshipDataValidator implements Consumer<User> {
  private final int MINIMUM_AGE = 18;

  @Override
  public void accept(User user) {
    int userAge = now().getYear() - user.getBirthDate().getYear();
    if (userAge >= MINIMUM_AGE && user.getNic() == null) {
      throw new ForbiddenException(
          "Please complete your information at the Administration to be able to get your"
              + " certificate.");
    }
  }
}
