package school.hei.haapi.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.UserMapper;
import school.hei.haapi.endpoint.rest.model.CrupdateMonitor;
import school.hei.haapi.endpoint.rest.model.Monitor;
import school.hei.haapi.endpoint.rest.validator.CoordinatesValidator;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class MonitorController {
  private final UserService userService;
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
    return userService
        .saveAll(
            toWrite.stream().map(userMapper::toDomain).collect(Collectors.toUnmodifiableList()))
        .stream()
        .map(userMapper::toRestMonitor)
        .collect(Collectors.toUnmodifiableList());
  }
}
