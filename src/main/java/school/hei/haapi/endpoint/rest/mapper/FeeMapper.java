package school.hei.haapi.endpoint.rest.mapper;

import static java.util.stream.Collectors.toUnmodifiableList;
import static school.hei.haapi.endpoint.rest.mapper.FileInfoMapper.ONE_DAY_DURATION_AS_LONG;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.UNPAID;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.*;
import school.hei.haapi.endpoint.rest.validator.CreateFeeValidator;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.service.LetterService;
import school.hei.haapi.service.UserService;
import school.hei.haapi.service.aws.FileService;
import school.hei.haapi.service.utils.DataFormatterUtils;

@Component
@AllArgsConstructor
public class FeeMapper {
  private final CreateFeeValidator createFeeValidator;
  private final MpbsMapper mpbsMapper;
  private final LetterService letterService;
  private final FileService fileService;
  private final UserService userService;

  public Fee toRestFee(school.hei.haapi.model.Fee fee) {
    Mpbs feeMpbs = fee.getMpbs() != null ? mpbsMapper.toRest(fee.getMpbs()) : null;
    var studentFee = fee.getStudent();
    var letter = letterService.getByFeeId(fee.getId());

    return new Fee()
        .id(fee.getId())
        .studentId(studentFee.getId())
        .studentRef(studentFee.getRef())
        .status(fee.getStatus())
        .type(fee.getType())
        .totalAmount(fee.getTotalAmount())
        .remainingAmount(fee.getRemainingAmount())
        .comment(fee.getComment())
        .mpbs(feeMpbs)
        .creationDatetime(fee.getCreationDatetime())
        .updatedAt(fee.getUpdatedAt())
        .dueDatetime(fee.getDueDatetime())
        .studentFirstName(fee.getStudent().getFirstName())
        .letter(letter == null ? null : toLetterFee(letter));
  }

  public FeeLetter toLetterFee(school.hei.haapi.model.Letter letter) {
    String fileUrl =
        letter.getFilePath() == null
            ? null
            : fileService.getPresignedUrl(letter.getFilePath(), ONE_DAY_DURATION_AS_LONG);
    return new FeeLetter()
        .id(letter.getId())
        .ref(letter.getRef())
        .status(letter.getStatus())
        .approvalDatetime(letter.getApprovalDatetime())
        .creationDatetime(letter.getCreationDatetime())
        .fileUrl(fileUrl);
  }

  public school.hei.haapi.model.Fee toDomain(Fee fee, User student) {
    FeeStatusEnum dueDatetimeDependantStatus =
        DataFormatterUtils.isLate(fee.getDueDatetime()) ? LATE : UNPAID;
    return school.hei.haapi.model.Fee.builder()
        .id(fee.getId())
        .student(student)
        .type(fee.getType())
        .totalAmount(fee.getTotalAmount())
        .updatedAt(Instant.now())
        .status(fee.getRemainingAmount() > 0 ? dueDatetimeDependantStatus : PAID)
        .remainingAmount(fee.getRemainingAmount())
        .comment(fee.getComment())
        .creationDatetime(fee.getCreationDatetime())
        .dueDatetime(fee.getDueDatetime())
        .build();
  }

  public school.hei.haapi.model.Fee ToDomain(CrupdateStudentFee crupdateFee) {
    User student = userService.findById(crupdateFee.getStudentId());
    school.hei.haapi.model.Fee fee =
        school.hei.haapi.model.Fee.builder()
            .id(crupdateFee.getId())
            .student(student)
            .type(crupdateFee.getType())
            .totalAmount(crupdateFee.getTotalAmount())
            .remainingAmount(crupdateFee.getTotalAmount())
            .comment(crupdateFee.getComment())
            .creationDatetime(crupdateFee.getCreationDatetime())
            .dueDatetime(crupdateFee.getDueDatetime())
            .build();
    if (crupdateFee.getId() != null) {
      fee.setUpdatedAt(Instant.now());
    }
    if (crupdateFee.getDueDatetime() != null) {
      fee.setStatus(DataFormatterUtils.isLate(crupdateFee.getDueDatetime()) ? LATE : UNPAID);
    }
    return fee;
  }

  private school.hei.haapi.model.Fee toDomainFee(User student, CreateFee createFee) {
    createFeeValidator.accept(createFee);
    if (!student.getRole().equals(User.Role.STUDENT)) {
      throw new BadRequestException("Only students can have fees");
    }
    school.hei.haapi.model.Fee fee =
        school.hei.haapi.model.Fee.builder()
            .student(student)
            .type(createFee.getType())
            .totalAmount(createFee.getTotalAmount())
            .updatedAt(createFee.getCreationDatetime())
            .remainingAmount(createFee.getTotalAmount())
            .comment(createFee.getComment())
            .creationDatetime(createFee.getCreationDatetime())
            .dueDatetime(createFee.getDueDatetime())
            .build();

    if (createFee.getDueDatetime() != null) {
      fee.setStatus(DataFormatterUtils.isLate(createFee.getDueDatetime()) ? LATE : UNPAID);
    }
    return fee;
  }

  public List<school.hei.haapi.model.Fee> toDomainFee(User student, List<CreateFee> toCreate) {
    if (student == null) {
      throw new NotFoundException("Student.id=" + student.getId() + " is not found");
    }
    return toCreate.stream()
        .map(createFee -> toDomainFee(student, createFee))
        .collect(toUnmodifiableList());
  }
}
