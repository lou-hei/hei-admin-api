package school.hei.haapi.endpoint.event;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import school.hei.haapi.endpoint.event.model.LateFeeVerified;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.model.User;
import school.hei.haapi.service.UserService;
import school.hei.haapi.service.event.LateFeeVerifiedService;
import school.hei.haapi.service.event.UserUpsertedService;
import school.hei.haapi.service.utils.Base64Converter;
import school.hei.haapi.service.utils.ClassPathResourceResolver;

class EventServiceInvokerTest {
  UserUpsertedService userUpsertedService;
  LateFeeVerifiedService lateFeeService;
  Mailer sesService;
  UserService userService;
  Base64Converter base64Converter;
  ClassPathResourceResolver classPathResourceResolver;

  static User randomStudent() {
    return User.builder()
        .lastName("user")
        .firstName("random")
        .email("test+" + randomUUID() + "@hei.school")
        .build();
  }

  static LateFeeVerified lateFee() {
    var student = randomStudent();
    return LateFeeVerified.builder()
        .student(LateFeeVerified.FeeUser.from(student))
        .remainingAmount(25_000)
        .dueDatetime(Instant.parse("2023-02-08T08:30:24Z"))
        .build();
  }

  @BeforeEach
  void setUp() {
    userUpsertedService = mock(UserUpsertedService.class);
    sesService = mock(Mailer.class);
    userService = mock(UserService.class);
    classPathResourceResolver = mock(ClassPathResourceResolver.class);
    base64Converter = mock(Base64Converter.class);
    lateFeeService = new LateFeeVerifiedService(sesService, userService);
  }

  @Test
  void lateFeeService_invokes_corresponding_service() {
    lateFeeService.accept(lateFee());

    verify(sesService, times(1)).accept(any());
  }
}
