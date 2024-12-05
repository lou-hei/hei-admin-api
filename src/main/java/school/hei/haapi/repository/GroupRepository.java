package school.hei.haapi.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
  @Query(
      nativeQuery = true,
      value =
          """
          WITH student_group_flow AS (
              SELECT
                  gf.group_id,
                  gf.student_id
              FROM
                  group_flow gf
              WHERE
                  gf.student_id = ?1
              GROUP BY
                  gf.group_id,
                  gf.student_id
              HAVING
                  --join count
                  SUM(CASE WHEN gf.group_flow_type = 'JOIN' THEN 1 ELSE 0 END) >
                  --leave count
                  SUM(CASE WHEN gf.group_flow_type = 'LEAVE' THEN 1 ELSE 0 END)
          )
          SELECT
              g.*
          FROM
              "group" g
                  INNER JOIN
              student_group_flow sgf
              ON
                  sgf.group_id = g.id
          """)
  List<Group> findByStudentId(String studentId);

  @Query(
      nativeQuery = true,
      value =
          """
          WITH student_group_flow AS (
              SELECT
                  gf.group_id,
                  gf.student_id
              FROM
                  group_flow gf
              WHERE
                  gf.student_id = ?1
              GROUP BY
                  gf.group_id,
                  gf.student_id
          )
          SELECT
              g.*
          FROM
              "group" g
                  INNER JOIN
              student_group_flow sgf
              ON
                  sgf.group_id = g.id
          """)
  List<Group> findAllFormerAndCurrentByStudentId(String studentId);
}
