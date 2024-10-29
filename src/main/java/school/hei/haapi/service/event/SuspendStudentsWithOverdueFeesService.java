package school.hei.haapi.service.event;

import static school.hei.haapi.model.User.Status.SUSPENDED;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.model.SuspendStudentsWithOverdueFees;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.dao.UserManagerDao;
import school.hei.haapi.service.UserService;

@Service
@AllArgsConstructor
public class SuspendStudentsWithOverdueFeesService
    implements Consumer<SuspendStudentsWithOverdueFees> {

  private static final Logger log =
      LoggerFactory.getLogger(SuspendStudentsWithOverdueFeesService.class);
  private final UserManagerDao userManagerDao;
  private final UserService userService;

  // Suspends students with overdue fees if it hasn't been done already.
  public void suspendStudentsWithUnpaidOrLateFee() {
    List<User> students = userService.getStudentsWithUnpaidOrLateFee();
    log.info(
        "List of student references with unpaid or late fees: {} ",
        students.stream().map(User::getRef).collect(Collectors.toList()));
    for (User student : students) {
      if (!SUSPENDED.equals(student.getStatus())) {
        userManagerDao.updateUserStatusById(SUSPENDED, student.getId());
        log.info("suspended student reference : {} ", student.getRef());
      }
    }
  }

  @Override
  public void accept(SuspendStudentsWithOverdueFees suspendStudentsWithOverdueFees) {
    suspendStudentsWithUnpaidOrLateFee();
  }
}
