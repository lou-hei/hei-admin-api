package school.hei.haapi.repository;

import java.util.LinkedHashSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, String> {
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
          ),
          student_group AS (SELECT
              g.*
          FROM
              "group" g
                  INNER JOIN
              student_group_flow sgf
              ON
                  sgf.group_id = g.id)
          SELECT p.*
          FROM "promotion" p
                  INNER JOIN
                  student_group sg
              ON
                  sg.promotion_id = p.id
          ORDER BY p.start_datetime DESC
          """)
  LinkedHashSet<Promotion> findAllPromotionsByStudentId(String studentId);
}
