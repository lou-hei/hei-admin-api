package school.hei.haapi.service;

import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PENDING;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.rest.model.MpbsStatus;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.MpbsRepository;

@Service
@AllArgsConstructor
@Slf4j
public class MpbsService {
  private final MpbsRepository mpbsRepository;
  private final FeeService feeService;
  private final MultipartFileConverter multipartFileConverter;

  public Mpbs saveMpbs(Mpbs mobilePaymentByStudentToSave) {
    Fee fee = mobilePaymentByStudentToSave.getFee();
    fee.setStatus(PENDING);
    mobilePaymentByStudentToSave.setFee(feeService.update(fee));
    return mpbsRepository.save(mobilePaymentByStudentToSave);
  }

  public Mpbs getStudentMobilePaymentByFeeId(String studentId, String feeId) {
    return mpbsRepository.findByStudentIdAndFeeId(studentId, feeId);
  }

  public Mpbs getByPspId(String pspId) {
    return mpbsRepository
        .findByPspId(pspId)
        .orElseThrow(() -> new NotFoundException("Mpbs not found #" + pspId));
  }

  public Long countPendingOfStudent(String studentId) {
    return mpbsRepository.countMpbsByStatusAndStudentId(MpbsStatus.PENDING, studentId);
  }
}
