package school.hei.haapi.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Grade;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.GradeRepository;
import school.hei.haapi.repository.dao.GradeDao;

@Service
@AllArgsConstructor
public class GradeService {
  private final GradeRepository gradeRepository;
  private final GradeDao gradeDao;

  public Grade getGradeByExamIdAndStudentId(String examId, String studentId) {
    return gradeRepository.getGradeByExamIdAndStudentIdAndAwardedCourseIdAndGroupId(
        examId, studentId);
  }

  public Grade getById(String id) {
    return gradeRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("grade with id " + id + " not found"));
  }

  public Grade getByStudentId(String id) {
    return gradeRepository.findByStudentId(id);
  }

  public Grade crupdateParticipantGrade(Grade grade) {
    return gradeRepository.save(grade);
  }

  public List<Grade> getParticipantsGradeForExam(
      String exam_id, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of((page.getValue() - 1), pageSize.getValue());
    return gradeDao.getGradesByExamId(exam_id, pageable);
  }
}
