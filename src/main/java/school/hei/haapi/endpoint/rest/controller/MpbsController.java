package school.hei.haapi.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.MpbsMapper;
import school.hei.haapi.endpoint.rest.model.CrupdateMpbs;
import school.hei.haapi.endpoint.rest.model.Mpbs;
import school.hei.haapi.endpoint.rest.validator.CreateMpbsValidator;
import school.hei.haapi.service.MpbsService;

@RestController
@AllArgsConstructor
public class MpbsController {
  private final CreateMpbsValidator validator;
  private final MpbsService mpbsService;
  private final MpbsMapper mapper;

  @PutMapping(value = "/students/{student_id}/fees/{fee_id}/mpbs")
  public Mpbs crupdateMpbs(
      @PathVariable(name = "student_id") String studentId,
      @PathVariable(name = "fee_id") String feeId,
      @RequestBody CrupdateMpbs mpbsToSave) {
    validator.accept(studentId, feeId, mpbsToSave);
    school.hei.haapi.model.Mpbs.Mpbs mappedMpbsToSave = mapper.toDomain(mpbsToSave);
    return mapper.toRest(mpbsService.saveMpbs(mappedMpbsToSave));
  }

  @GetMapping(value = "/students/{student_id}/fees/{fee_id}/mpbs")
  public Mpbs getMpbs(
      @PathVariable(name = "student_id") String studentId,
      @PathVariable(name = "fee_id") String feeId) {
    return mapper.toRest(mpbsService.getStudentMobilePaymentByFeeId(studentId, feeId));
  }
}
