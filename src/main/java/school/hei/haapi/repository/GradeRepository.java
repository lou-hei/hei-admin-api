package school.hei.haapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.Grade;

@Repository
public interface GradeRepository extends JpaRepository<Grade, String> {
  @Query("select g from Grade g where  g.exam.id = :exam_id and g.student.id = :student_id")
  Grade getGradeByExamIdAndStudentIdAndAwardedCourseIdAndGroupId(
      @Param("exam_id") String examId, @Param("student_id") String studentId);

  Grade findByStudentId(String studentId);
}
