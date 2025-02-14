package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;
import static school.hei.haapi.model.User.Role.GATE_KEEPER;

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
import school.hei.haapi.endpoint.rest.model.CrupdateGateKeeper;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.GateKeeper;
import school.hei.haapi.endpoint.rest.model.Sex;
import school.hei.haapi.endpoint.rest.validator.CoordinatesValidator;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.User;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class GateKeeperController {
  private final UserService userService;
  private final StatusEnumMapper statusEnumMapper;
  private final SexEnumMapper sexEnumMapper;
  private final UserMapper userMapper;
  private final CoordinatesValidator validator;

  @GetMapping("/gate_keepers")
  public List<GateKeeper> getGateKeepers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(name = "status", required = false) EnableStatus status,
      @RequestParam(name = "sex", required = false) Sex sex,
      @RequestParam(value = "first_name", required = false, defaultValue = "") String firstName,
      @RequestParam(value = "last_name", required = false, defaultValue = "") String lastName,
      @RequestParam(value = "ref", required = false, defaultValue = "") String ref) {
    User.Status domainStatus = statusEnumMapper.toDomainStatus(status);
    User.Sex domainSexEnum = sexEnumMapper.toDomainSexEnum(sex);
    return userService
        .getByCriteria(
            GATE_KEEPER, firstName, lastName, ref, page, pageSize, domainStatus, domainSexEnum)
        .stream()
        .map(userMapper::toRestGateKeeper)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/gate_keepers/{id}")
  public GateKeeper getGateKeeperById(@PathVariable String id) {
    return userMapper.toRestGateKeeper(userService.findById(id));
  }

  @PutMapping("/gate_keepers")
  public List<GateKeeper> crupdateGateKeepers(
      @RequestBody List<CrupdateGateKeeper> crupdateGateKeeper) {
    crupdateGateKeeper.forEach(gateKeeper -> validator.accept(gateKeeper.getCoordinates()));
    return userService
        .saveAll(
            crupdateGateKeeper.stream().map(userMapper::toDomain).collect(toUnmodifiableList()))
        .stream()
        .map(userMapper::toRestGateKeeper)
        .collect(toUnmodifiableList());
  }
}
