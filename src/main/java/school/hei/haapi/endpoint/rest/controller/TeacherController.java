package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.util.List;
import lombok.AllArgsConstructor;
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
import school.hei.haapi.endpoint.rest.model.CrupdateTeacher;
import school.hei.haapi.endpoint.rest.model.EnableStatus;
import school.hei.haapi.endpoint.rest.model.Sex;
import school.hei.haapi.endpoint.rest.model.Teacher;
import school.hei.haapi.endpoint.rest.validator.CoordinatesValidator;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.User;
import school.hei.haapi.service.UserService;
import school.hei.haapi.service.aws.FileService;

@RestController
@AllArgsConstructor
public class TeacherController {
  private final SexEnumMapper sexEnumMapper;
  private final StatusEnumMapper statusEnumMapper;
  private final UserService userService;
  private final UserMapper userMapper;
  private final FileService fileService;
  private final CoordinatesValidator validator;

  @GetMapping(value = "/teachers/{id}")
  public Teacher getTeacherById(@PathVariable String id) {
    return userMapper.toRestTeacher(userService.findById(id));
  }

  @PutMapping("/teachers/{id}")
  public Teacher updateTeacher(
      @PathVariable(name = "id") String teacherId, @RequestBody CrupdateTeacher toUpdate) {
    validator.accept(toUpdate.getCoordinates());
    return userMapper.toRestTeacher(
        userService.updateUser(userMapper.toDomain(toUpdate), teacherId));
  }

  @GetMapping(value = "/teachers")
  public List<Teacher> getTeachers(
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize,
      @RequestParam(value = "ref", required = false, defaultValue = "") String ref,
      @RequestParam(value = "first_name", required = false, defaultValue = "") String firstName,
      @RequestParam(value = "last_name", required = false, defaultValue = "") String lastName,
      @RequestParam(name = "status", required = false) EnableStatus status,
      @RequestParam(name = "sex", required = false) Sex sex) {
    User.Sex domainSex = sex != null ? sexEnumMapper.toDomainSexEnum(sex) : null;
    User.Status domainStatus = status != null ? statusEnumMapper.toDomainStatus(status) : null;
    return userService
        .getByCriteria(
            User.Role.TEACHER, firstName, lastName, ref, page, pageSize, domainStatus, domainSex)
        .stream()
        .map(userMapper::toRestTeacher)
        .collect(toUnmodifiableList());
  }

  @PutMapping(value = "/teachers")
  public List<Teacher> createOrUpdateTeachers(@RequestBody List<CrupdateTeacher> toWrite) {
    toWrite.forEach(teacher -> validator.accept(teacher.getCoordinates()));
    return userService
        .saveAll(toWrite.stream().map(userMapper::toDomain).collect(toUnmodifiableList()))
        .stream()
        .map(userMapper::toRestTeacher)
        .collect(toUnmodifiableList());
  }

  @PostMapping(value = "/teachers/{id}/picture/raw", consumes = MULTIPART_FORM_DATA_VALUE)
  public Teacher uploadTeacherProfilePicture(
      @RequestPart("file_to_upload") MultipartFile profilePictureAsMultipartFile,
      @PathVariable String id) {
    userService.uploadUserProfilePicture(profilePictureAsMultipartFile, id);
    return userMapper.toRestTeacher(userService.findById(id));
  }
}
