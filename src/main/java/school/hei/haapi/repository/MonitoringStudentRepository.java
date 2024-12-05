package school.hei.haapi.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.User;

@Repository
public interface MonitoringStudentRepository extends JpaRepository<User, String> {
  @Query(
      "SELECT m FROM User m JOIN m.monitors s WHERE s.id = :studentId AND m.role = 'MONITOR' AND"
          + " s.role = 'STUDENT'")
  List<User> findAllMonitorsByStudentId(@Param("studentId") String studentId);

  @Query(
      value =
          """
          SELECT u.* FROM "user" u
            LEFT JOIN monitor_following_student mfs ON u.id = mfs.student_id
            WHERE mfs.monitor_id = :monitorId
            AND u.role = 'STUDENT'
          """,
      nativeQuery = true)
  List<User> findAllStudentsByMonitorId(@Param("monitorId") String monitorId, Pageable pageable);

  @Modifying
  @Query(
      value =
          "INSERT INTO monitor_following_student (student_id, monitor_id) VALUES (:studentId,"
              + " :monitorId)",
      nativeQuery = true)
  void saveMonitorFollowingStudents(
      @Param("monitorId") String monitorId, @Param("studentId") String studentId);
}
