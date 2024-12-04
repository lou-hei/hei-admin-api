package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.TUITION;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.model.User.Role.STUDENT;
import static school.hei.haapi.model.User.Status.ENABLED;
import static school.hei.haapi.model.User.Status.SUSPENDED;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.model.LateFeeVerified;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.mail.Email;
import school.hei.haapi.mail.Mailer;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.UserRepository;
import school.hei.haapi.service.event.LateFeeVerifiedService;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = LateFeeVerifiedServiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
public class LateFeeVerifiedServiceIT extends MockedThirdParties {
  @Autowired private LateFeeVerifiedService subject;
  @Autowired private UserRepository userRepository;
  @MockBean private Mailer mailer;

  private static User domainUser() {
    User user = new User();
    user.setEmail("email@gmail.com");
    user.setNic("nic");
    user.setStatus(ENABLED);
    user.setAddress("Adr 12");
    user.setLastName("Lastname");
    user.setFirstName("Firstname");
    user.setBirthPlace("Birth place");
    user.setEntranceDatetime(Instant.now());
    user.setRef("Ref STD");
    user.setRole(STUDENT);

    return user;
  }

  @BeforeEach
  void setUp() {
    doNothing().when(mailer).accept(any(Email.class));
  }

  @Test
  void late_fee_sends_mail_and_suspend_student_ok() {
    User storedUser = userRepository.save(domainUser());
    // test: user saved is not flagged suspended
    assertEquals(ENABLED, storedUser.getStatus());

    LateFeeVerified subjectLateFeeVerified =
        LateFeeVerified.builder()
            .student(LateFeeVerified.FeeUser.from(storedUser))
            .remainingAmount(3000)
            .comment("Fee late")
            .dueDatetime(Instant.parse("2022-10-09T08:25:24.00Z"))
            .type(TUITION)
            .build();

    subject.accept(subjectLateFeeVerified);
    // test: mail is correctly executed
    verify(mailer).accept(any(Email.class));

    User actualStudent = userRepository.findById(storedUser.getId()).get();
    // test: user is correctly flagged to suspended
    assertEquals(SUSPENDED, actualStudent.getStatus());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
