package school.hei.haapi.endpoint.rest.mapper;

import static java.time.Instant.now;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.PENDING;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.CrupdateMpbs;
import school.hei.haapi.endpoint.rest.model.Mpbs;
import school.hei.haapi.service.FeeService;
import school.hei.haapi.service.UserService;

@Component
@AllArgsConstructor
public class MpbsMapper {
  private final UserService userService;
  private final FeeService feeService;

  public Mpbs toRest(school.hei.haapi.model.Mpbs.Mpbs domain) {
    return new Mpbs()
        .id(domain.getId())
        .studentId(domain.getStudent().getId())
        .creationDatetime(domain.getCreationDatetime())
        .feeId(domain.getFee().getId())
        .pspId(domain.getPspId())
        .pspType(domain.getMobileMoneyType())
        .amount(domain.getAmount())
        .successfullyVerifiedOn(domain.getSuccessfullyVerifiedOn())
        .lastDatetimeVerification(domain.getLastVerificationDatetime())
        .pspOwnDatetimeVerification(domain.getPspOwnDatetimeVerification())
        .status(domain.getStatus());
  }

  public school.hei.haapi.model.Mpbs.Mpbs toDomain(CrupdateMpbs rest) {
    school.hei.haapi.model.Mpbs.Mpbs domain = new school.hei.haapi.model.Mpbs.Mpbs();

    domain.setId(rest.getId());
    domain.setStudent(userService.findById(rest.getStudentId()));
    domain.setFee(feeService.getById(rest.getFeeId()));
    domain.setPspId(rest.getPspId());
    domain.setMobileMoneyType(rest.getPspType());
    domain.setCreationDatetime(now());
    domain.setStatus(PENDING);
    return domain;
  }
}
