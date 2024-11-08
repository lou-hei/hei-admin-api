package school.hei.haapi.service.event;

import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.model.CheckSuspendedStudentsStatus;
import school.hei.haapi.model.User;
import school.hei.haapi.service.PaymentService;
import school.hei.haapi.service.UserService;

@Service
@AllArgsConstructor
@Slf4j
public class CheckSuspendedStudentsStatusService implements Consumer<CheckSuspendedStudentsStatus> {

  private final UserService userService;
  private final PaymentService paymentService;

  // If the student has no more overdue fees, their status will be set to ENABLED, otherwise it will
  // remain SUSPENDED.
  public void updateStatusBasedOnPayment() {
    List<User> suspendedStudents = userService.getAllSuspendedUsers();
    log.info("suspended students size = {}", suspendedStudents.size());
    for (User student : suspendedStudents) {
      paymentService.computeUserStatusAfterPayingFee(student);
    }
  }

  @Override
  public void accept(CheckSuspendedStudentsStatus checkSuspendedStudentsStatus) {
    updateStatusBasedOnPayment();
  }
}
