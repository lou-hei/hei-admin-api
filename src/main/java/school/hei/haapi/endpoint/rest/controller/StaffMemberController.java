package school.hei.haapi.endpoint.rest.controller;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static school.hei.haapi.model.User.Role.STAFF_MEMBER;

import java.util.List;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.rest.mapper.SexEnumMapper;
import school.hei.haapi.endpoint.rest.mapper.StatusEnumMapper;
import school.hei.haapi.endpoint.rest.mapper.UserMapper;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.Sex;
import school.hei.haapi.endpoint.rest.model.StaffMember;
import school.hei.haapi.endpoint.rest.validator.CoordinatesValidator;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.User;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class StaffMemberController {

  private static final Logger log = LoggerFactory.getLogger(StaffMemberController.class);
  private final UserService userService;
  private final UserMapper userMapper;
  private final SexEnumMapper sexEnumMapper;
  private final StatusEnumMapper statusEnumMapper;
  private final CoordinatesValidator validator;

  @GetMapping("/staff_members")
  public List<StaffMember> getStaffMembers(
      @RequestParam(name = "sex", required = false) Sex sex,
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(value = "first_name", required = false, defaultValue = "") String firstName,
      @RequestParam(value = "last_name", required = false, defaultValue = "") String lastName,
      @RequestParam(name = "status", required = false) EnableStatus status) {
    User.Sex domainSex = sexEnumMapper.toDomainSexEnum(sex);
    User.Status domainStatus = statusEnumMapper.toDomainStatus(status);
    return userService
        .getByCriteria(
            STAFF_MEMBER, firstName, lastName, "", page, pageSize, domainStatus, domainSex)
        .stream()
        .map(userMapper::toRestStaffMember)
        .toList();
  }

  @GetMapping("/staff_members/xlsx/raw")
  public byte[] getStaffMembersXlsx() {
    return userService.getByRoleAndStatusAsXlsx(
        STAFF_MEMBER, User.Status.ENABLED, userMapper::toRestStaffMember);
  }

  @PostMapping(value = "/staff_members/{id}/picture/raw", consumes = MULTIPART_FORM_DATA_VALUE)
  public StaffMember uploadStaffMembersProfilePicture(
      @RequestPart("file_to_upload") MultipartFile profilePictureAsMultipartFile,
      @PathVariable String id) {
    userService.uploadUserProfilePicture(profilePictureAsMultipartFile, id);
    return userMapper.toRestStaffMember(userService.findById(id));
  }

  @PutMapping("/staff_members")
  public List<StaffMember> createStaffMembers(@RequestBody List<StaffMember> toWrite) {
    toWrite.forEach(student -> validator.accept(student.getCoordinates()));
    return userService.saveAll(toWrite.stream().map(userMapper::toDomain).toList()).stream()
        .map(userMapper::toRestStaffMember)
        .toList();
  }

  @GetMapping("/staff_members/{id}")
  public StaffMember getStaffMemberById(@PathVariable("id") String id) {
    return userMapper.toRestStaffMember(userService.findById(id));
  }

  @PutMapping("/staff_members/{id}")
  public StaffMember updateStaffMemberById(
      @PathVariable("id") String id, @RequestBody StaffMember staffMember) {
    validator.accept(staffMember.getCoordinates());
    return userMapper.toRestStaffMember(
        userService.updateUser(userMapper.toDomain(staffMember), id));
  }
}
