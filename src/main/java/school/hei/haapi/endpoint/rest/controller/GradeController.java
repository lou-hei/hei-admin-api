package school.hei.haapi.endpoint.rest.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import school.hei.haapi.endpoint.rest.mapper.AwardedCourseMapper;
import school.hei.haapi.endpoint.rest.mapper.GradeMapper;
import school.hei.haapi.endpoint.rest.model.AwardedCourseExam;
import school.hei.haapi.endpoint.rest.model.CrupdateGrade;
import school.hei.haapi.endpoint.rest.model.GetStudentGrade;
import school.hei.haapi.endpoint.rest.validator.GradeValidator;
import school.hei.haapi.model.AwardedCourse;
import school.hei.haapi.model.Grade;
import school.hei.haapi.model.User;
import school.hei.haapi.service.AwardedCourseService;
import school.hei.haapi.service.GradeService;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class GradeController {
  private final UserService userService;
  private final AwardedCourseService awardedCourseService;
  private final AwardedCourseMapper awardedCourseMapper;
  private final GradeValidator validator;
  private final GradeService gradeService;
  private final GradeMapper gradeMapper;

  // todo: to review all class
  @GetMapping("/students/{student_id}/grades")
  public List<AwardedCourseExam> getAllGradesOfStudent(
      @PathVariable("student_id") String studentId) {
    List<AwardedCourse> awardedCourses = awardedCourseService.getByStudentId(studentId);
    User student = userService.findById(studentId);
    return awardedCourseMapper.toRest(awardedCourses, student);
  }

  //  @GetMapping(
  //      value = "/groups/{group_id}/awarded_courses/" +
  // "{awarded_course_id}/exams/{exam_id}/grades")
  //  public ExamDetail getExamGrades(
  //      @PathVariable("group_id") String groupId,
  //      @PathVariable("awarded_course_id") String awardedCourseId,
  //      @PathVariable("exam_id") String examId) {
  //    List<Grade> grades =
  //        examService
  //            .getExamsByIdAndGroupIdAndAwardedCourseId(examId, awardedCourseId, groupId)
  //            .getGrades();
  //    Exam exam =
  //        examService.getExamsByIdAndGroupIdAndAwardedCourseId(examId, awardedCourseId, groupId);
  //    return gradeMapper.toRestExamDetail(exam, grades);
  //  }

  // TODO: change that if null
  @GetMapping(value = "/exams/{exam_id}/students/{student_id}/grade")
  public GetStudentGrade getGradeOfStudentInOneExam(
      @PathVariable("exam_id") String examId, @PathVariable("student_id") String studentId) {
    Grade grade = gradeService.getGradeByExamIdAndStudentId(examId, studentId);
    return gradeMapper.toRestStudentGrade(grade);
  }

  @PutMapping(value = "/exams/{exam_id}/students/{student_id}/grade")
  public school.hei.haapi.endpoint.rest.model.Grade crupdateParticipantGrade(
      @PathVariable("exam_id") String examId,
      @PathVariable("student_id") String studentId,
      @RequestBody CrupdateGrade grade) {
      validator.accept(grade);
      Grade to_save = gradeMapper.toDomain(grade, examId, studentId);
      return gradeMapper.toRest(gradeService.crupdateParticipantGrade(to_save));
  }
}
