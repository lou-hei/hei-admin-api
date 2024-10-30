package school.hei.haapi.endpoint.rest.mapper;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.GetStudentGrade;
import school.hei.haapi.endpoint.rest.model.Grade;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.model.Exam;
import school.hei.haapi.model.User;
import school.hei.haapi.service.ExamService;
import school.hei.haapi.service.UserService;

@Component
@AllArgsConstructor
public class GradeMapper {
  private final UserMapper userMapper;
  private final ExamService examService;
  private final UserService userService;

  // todo: to review all class
  public school.hei.haapi.model.Grade toDomain(Grade grade) {
    return school.hei.haapi.model.Grade.builder()
        .score(grade.getScore().intValue())
        .creationDatetime(grade.getCreatedAt())
        .build();
  }

  public Grade toRest(school.hei.haapi.model.Grade grade) {
    return new Grade()
        .id(grade.getId())
        .createdAt(grade.getCreationDatetime())
        .score(grade.getScore().doubleValue())
        .updateDate(grade.getCreationDatetime());
  }

  public GetStudentGrade toRestStudentGrade(school.hei.haapi.model.Grade grade) {
    if (grade == null) {
      return null;
    }
    var getStudentGrade = new GetStudentGrade().grade(toRest(grade));
    getStudentGrade.setStudent(userMapper.toRestStudent(grade.getStudent()));

    return getStudentGrade;
  }

  public GetStudentGrade toRestStudentExamGrade(User student, Exam exam) {
    Optional<school.hei.haapi.model.Grade> optionalGrade =
        exam.getGrades().stream()
            .filter(grade -> grade.getStudent().getId().equals(student.getId()))
            .findFirst();
    school.hei.haapi.model.Grade grade = optionalGrade.get();
    var getStudentGrade = new GetStudentGrade().grade(toRest(grade));
    getStudentGrade.setStudent(userMapper.toRestStudent(student));
    return getStudentGrade;
  }

  //  public ExamDetail toRestExamDetail(Exam exam, List<school.hei.haapi.model.Grade> grades) {
  //    return new ExamDetail()
  //        .id(exam.getId())
  //        .coefficient(exam.getCoefficient())
  //        .title(exam.getTitle())
  //        .examinationDate(exam.getExaminationDate().atZone(ZoneId.systemDefault()).toInstant())
  //        .participants(
  //            grades.stream().map(grade -> this.toRestStudentGrade(grade)).collect(toList()));
  //  }

  public school.hei.haapi.model.Grade toRest(Grade grade, String examId, String studentId){
    User student = userService.findById(studentId);
    Exam exam = examService.getExamById(examId);
    return school.hei.haapi.model.Grade.builder()
            .id(grade.getId())
            .exam(exam)
            .student(student)
            .creationDatetime(grade.getCreatedAt())
            .score(grade.getScore().intValue())
            .build();
  }

  public Grade toRestGrade(Grade grade){
    Grade grade1 = new Grade();
    grade1.id(grade.getId());
    grade1.score(grade.getScore());
    return grade1;
  }
}
