package school.hei.haapi.service.event;

import static school.hei.haapi.model.User.Status.SUSPENDED;

import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.model.SuspendStudentsWithOverdueFees;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.dao.UserManagerDao;
import school.hei.haapi.service.MpbsService;
import school.hei.haapi.service.UserService;

@Service
@AllArgsConstructor
public class SuspendStudentsWithOverdueFeesService
    implements Consumer<SuspendStudentsWithOverdueFees> {

  private static final Logger log =
      LoggerFactory.getLogger(SuspendStudentsWithOverdueFeesService.class);
  private final UserManagerDao userManagerDao;
  private final UserService userService;
  private final MpbsService mpbsService;

  // Suspends students with overdue fees if it hasn't been done already.
  public void suspendStudentsWithUnpaidOrLateFee() {
    List<User> students = userService.getStudentsWithUnpaidOrLateFee();
    log.info("list of student with unpaid or late fee : {} ", students);
    for (User student : students) {
      if (!SUSPENDED.equals(student.getStatus())) {
        Long studentMpbsPendingCount = mpbsService.countPendingOfStudent(student.getId());
        if (studentMpbsPendingCount == 0) {
          userManagerDao.updateUserStatusById(SUSPENDED, student.getId());
          log.info("suspended student : {} ", userService.findById(student.getId()));
        }
      }
    }
  }

  @Override
  public void accept(SuspendStudentsWithOverdueFees suspendStudentsWithOverdueFees) {
    suspendStudentsWithUnpaidOrLateFee();
  }
}
