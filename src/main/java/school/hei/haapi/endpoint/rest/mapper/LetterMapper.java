package school.hei.haapi.endpoint.rest.mapper;

import static school.hei.haapi.endpoint.rest.mapper.FileInfoMapper.ONE_DAY_DURATION_AS_LONG;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.Letter;
import school.hei.haapi.endpoint.rest.model.LetterFee;
import school.hei.haapi.endpoint.rest.model.LetterUser;
import school.hei.haapi.endpoint.rest.model.RoleParamEnum;
import school.hei.haapi.model.User;
import school.hei.haapi.service.aws.FileService;

@Component
@AllArgsConstructor
public class LetterMapper {

  private final FileService fileService;

  public User.Role toDomainStatus(RoleParamEnum role) {
    return switch (role) {
      case STUDENT -> User.Role.STUDENT;
      case TEACHER -> User.Role.TEACHER;
      case STAFF_MEMBER -> User.Role.STAFF_MEMBER;
    };
  }

  public Letter toRest(school.hei.haapi.model.Letter domain) {
    String letterFileUrl =
        domain.getFilePath() != null
            ? fileService.getPresignedUrl(domain.getFilePath(), ONE_DAY_DURATION_AS_LONG)
            : null;

    User student = domain.getUser();
    String picUrl =
        student.getProfilePictureKey() != null
            ? fileService.getPresignedUrl(student.getProfilePictureKey(), ONE_DAY_DURATION_AS_LONG)
            : null;

    return new Letter()
        .id(domain.getId())
        .description(domain.getDescription())
        .creationDatetime(domain.getCreationDatetime())
        .approvalDatetime(domain.getApprovalDatetime())
        .status(domain.getStatus())
        .ref(domain.getRef())
        .user(
            new LetterUser()
                .id(student.getId())
                .ref(student.getRef())
                .email(student.getEmail())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .nic(student.getNic())
                .profilePicture(picUrl))
        .fileUrl(letterFileUrl)
        .reasonForRefusal(domain.getReasonForRefusal())
        .fee(
            domain.getFee() != null
                ? new LetterFee()
                    .id(domain.getFee().getId())
                    .comment(domain.getFee().getComment())
                    .type(domain.getFee().getType())
                    .amount(domain.getFee().getTotalAmount())
                : null);
  }
}
