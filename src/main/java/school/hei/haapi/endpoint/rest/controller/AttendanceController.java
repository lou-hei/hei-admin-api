package school.hei.haapi.endpoint.rest.controller;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.AttendanceMapper;
import school.hei.haapi.endpoint.rest.model.AttendanceStatus;
import school.hei.haapi.endpoint.rest.model.CreateAttendanceMovement;
import school.hei.haapi.endpoint.rest.model.StudentAttendance;
import school.hei.haapi.endpoint.rest.model.StudentAttendanceMovement;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.AttendanceService;

@RestController
@AllArgsConstructor
public class AttendanceController {
  private final AttendanceMapper attendanceMapper;
  private final AttendanceService attendanceService;

  @PostMapping("/attendance/movement")
  public List<StudentAttendanceMovement> createAttendanceMovement(
      @RequestBody List<CreateAttendanceMovement> movement) {
    return attendanceService
        .createStudentAttendanceMovement(movement.stream().map(attendanceMapper::toDomain).toList())
        .stream()
        .map(attendanceMapper::toRestMovement)
        .toList();
  }

  @GetMapping("/attendance")
  public List<StudentAttendance> getStudentsAttendance(
      @RequestParam(name = "page") PageFromOne page,
      @RequestParam(name = "page_size") BoundedPageSize pageSize,
      @RequestParam(name = "courses_ids", required = false) List<String> coursesIds,
      @RequestParam(name = "teachers_ids", required = false) List<String> teacherIds,
      @RequestParam(name = "student_key_word", required = false, defaultValue = "")
          String studentKeyWord,
      @RequestParam(name = "from", required = false) Instant from,
      @RequestParam(name = "to", required = false) Instant to,
      @RequestParam(name = "attendance_statuses", required = false)
          List<AttendanceStatus> attendanceStatuses) {
    return attendanceService
        .getStudentAttendances(
            studentKeyWord, coursesIds, teacherIds, attendanceStatuses, from, to, page, pageSize)
        .stream()
        .map(attendanceMapper::toRestAttendance)
        .toList();
  }
}
