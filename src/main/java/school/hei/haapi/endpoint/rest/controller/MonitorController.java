package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.UserMapper;
import school.hei.haapi.endpoint.rest.model.CrupdateMonitor;
import school.hei.haapi.endpoint.rest.model.Monitor;
import school.hei.haapi.endpoint.rest.validator.CoordinatesValidator;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.User;
import school.hei.haapi.service.MonitoringStudentService;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class MonitorController {
  private final UserService userService;
  private final MonitoringStudentService monitoringStudentService;
  private final UserMapper userMapper;
  private final CoordinatesValidator validator;

  @GetMapping(value = "/monitors/{id}")
  public Monitor getMonitorById(@PathVariable String id) {
    return userMapper.toRestMonitor(userService.findById(id));
  }

  @PutMapping("/monitors/{id}")
  public Monitor updateMonitorById(
      @PathVariable(name = "id") String monitorId, @RequestBody CrupdateMonitor toUpdate) {
    validator.accept(toUpdate.getCoordinates());
    return userMapper.toRestMonitor(
        userService.updateUser(userMapper.toDomain(toUpdate), monitorId));
  }

  @PutMapping(value = "/monitors")
  public List<Monitor> createOrUpdateMonitors(@RequestBody List<CrupdateMonitor> toWrite) {
    toWrite.forEach(monitor -> validator.accept(monitor.getCoordinates()));
    return monitoringStudentService.crupdateAndLinkMonitorFollowingStudents(toWrite).stream()
        .map(userMapper::toRestMonitor)
        .collect(toUnmodifiableList());
  }

  @GetMapping(value = "/monitors")
  public List<Monitor> getTeachers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(value = "ref", required = false, defaultValue = "") String ref,
      @RequestParam(value = "first_name", required = false, defaultValue = "") String firstName,
      @RequestParam(value = "last_name", required = false, defaultValue = "") String lastName) {
    return userService
        .getByCriteria(User.Role.MONITOR, firstName, lastName, ref, page, pageSize)
        .stream()
        .map(userMapper::toRestMonitor)
        .collect(toUnmodifiableList());
  }
}
