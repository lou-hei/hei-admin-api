package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;
import static school.hei.haapi.model.User.Role.ORGANIZER;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.SexEnumMapper;
import school.hei.haapi.endpoint.rest.mapper.StatusEnumMapper;
import school.hei.haapi.endpoint.rest.mapper.UserMapper;
import school.hei.haapi.endpoint.rest.model.CrupdateOrganizer;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.Organizer;
import school.hei.haapi.endpoint.rest.model.Sex;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.User;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class OrganizerController {
  private final UserService userService;
  private final UserMapper userMapper;
  private final SexEnumMapper sexEnumMapper;
  private final StatusEnumMapper statusEnumMapper;

  @GetMapping("/organizers")
  public List<Organizer> getOrganizers(
      @RequestParam("page") PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(value = "status", required = false) EnableStatus status,
      @RequestParam(value = "sex", required = false) Sex sex,
      @RequestParam(value = "first_name", required = false, defaultValue = "") String firstName,
      @RequestParam(value = "last_name", required = false, defaultValue = "") String lastName,
      @RequestParam(value = "ref", required = false, defaultValue = "") String ref) {
    User.Sex domainSex = sexEnumMapper.toDomainSexEnum(sex);
    User.Status domainStatus = statusEnumMapper.toDomainStatus(status);
    return userService
        .getByCriteria(ORGANIZER, firstName, lastName, ref, page, pageSize, domainStatus, domainSex)
        .stream()
        .map(userMapper::toRestOrganizer)
        .collect(toUnmodifiableList());
  }

  @PutMapping("/organizers")
  public List<Organizer> crupdateOrganizers(@RequestBody List<CrupdateOrganizer> organizers) {
    return userService
        .saveAll(organizers.stream().map(userMapper::toDomain).collect(toUnmodifiableList()))
        .stream()
        .map(userMapper::toRestOrganizer)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/organizers/{id}")
  public Organizer getOrganizerById(@PathVariable("id") String id) {
    return userMapper.toRestOrganizer(userService.findById(id));
  }
}
