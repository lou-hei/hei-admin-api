package school.hei.haapi.endpoint.rest.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import school.hei.haapi.endpoint.rest.mapper.AwardedCourseMapper;
import school.hei.haapi.endpoint.rest.mapper.GradeMapper;
import school.hei.haapi.endpoint.rest.model.AwardedCourseExam;
import school.hei.haapi.endpoint.rest.model.GetStudentGrade;
import school.hei.haapi.model.AwardedCourse;
import school.hei.haapi.model.Grade;
import school.hei.haapi.model.User;
import school.hei.haapi.service.AwardedCourseService;
import school.hei.haapi.service.ExamService;
import school.hei.haapi.service.GradeService;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class GradeController {
  private final UserService userService;
  private final AwardedCourseService awardedCourseService;
  private final AwardedCourseMapper awardedCourseMapper;
  private final GradeService gradeService;
  private final GradeMapper gradeMapper;
  private final ExamService examService;

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
          @PathVariable("exam_id") String examId, @PathVariable("student_id") String studentId,
          @RequestBody school.hei.haapi.endpoint.rest.model.Grade grade) {
    return new school.hei.haapi.endpoint.rest.model.Grade();
  }
}
