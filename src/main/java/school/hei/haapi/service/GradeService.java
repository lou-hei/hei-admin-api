package school.hei.haapi.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.Grade;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.GradeRepository;

@Service
@AllArgsConstructor
public class GradeService {
  // todo: to review all class
  private final GradeRepository gradeRepository;

  public Grade getGradeByExamIdAndStudentId(String examId, String studentId) {
    return gradeRepository.getGradeByExamIdAndStudentIdAndAwardedCourseIdAndGroupId(
        examId, studentId);
  }

  public Grade getById(String id) {
    return gradeRepository.findById(id).orElseThrow(
            ()-> new NotFoundException("grade with id "+id+" not found"));
  }

  public Grade getByStudentId(String id) {
    return gradeRepository.findByStudentId(id);
  }

  public Grade crupdateParticipantGrade(Grade grade) {
    return gradeRepository.save(grade);
  }
}
