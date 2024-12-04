package school.hei.haapi.endpoint.rest.controller;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.rest.mapper.UserMapper;
import school.hei.haapi.endpoint.rest.model.Admin;
import school.hei.haapi.endpoint.rest.model.CrupdateManager;
import school.hei.haapi.endpoint.rest.validator.CoordinatesValidator;
import school.hei.haapi.service.UserService;

@RestController
@AllArgsConstructor
public class AdminController {

  private final UserService userService;
  private final UserMapper userMapper;
  private final CoordinatesValidator validator;

  @GetMapping(value = "/admins/{id}")
  public Admin getAdminById(@PathVariable String id) {
    return userMapper.toRestAdmin(userService.findById(id));
  }

  @PutMapping("/admins/{id}")
  public Admin updateAdmin(
      @PathVariable(name = "id") String adminId, @RequestBody CrupdateManager toUpdate) {
    validator.accept(toUpdate.getCoordinates());
    return userMapper.toRestAdmin(userService.updateUser(userMapper.toDomain(toUpdate), adminId));
  }

  @PostMapping(value = "/admins/{id}/picture/raw", consumes = MULTIPART_FORM_DATA_VALUE)
  public Admin uploadAdminProfilePicture(
      @RequestPart("file_to_upload") MultipartFile profilePictureAsMultipartFile,
      @PathVariable String id) {
    userService.uploadUserProfilePicture(profilePictureAsMultipartFile, id);
    return userMapper.toRestAdmin(userService.findById(id));
  }
}
