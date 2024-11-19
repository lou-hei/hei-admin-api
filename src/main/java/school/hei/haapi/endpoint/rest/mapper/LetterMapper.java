package school.hei.haapi.endpoint.rest.mapper;

import static school.hei.haapi.endpoint.rest.mapper.FileInfoMapper.ONE_DAY_DURATION_AS_LONG;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.Letter;
import school.hei.haapi.endpoint.rest.model.LetterFee;
import school.hei.haapi.endpoint.rest.model.LetterStudent;
import school.hei.haapi.model.User;
import school.hei.haapi.service.aws.FileService;

@Component
@AllArgsConstructor
public class LetterMapper {

  private final UserMapper userMapper;
  private final FileService fileService;

  public Letter toRest(school.hei.haapi.model.Letter domain) {
    String letterFileUrl =
        domain.getFilePath() != null
            ? fileService.getPresignedUrl(domain.getFilePath(), ONE_DAY_DURATION_AS_LONG)
            : null;

    User student = domain.getStudent();
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
        .student(
            new LetterStudent()
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
