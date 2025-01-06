package school.hei.haapi.endpoint.rest.controller;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.rest.mapper.MpbsMapper;
import school.hei.haapi.endpoint.rest.model.CrupdateMpbs;
import school.hei.haapi.endpoint.rest.model.Mpbs;
import school.hei.haapi.endpoint.rest.validator.CreateMpbsValidator;
import school.hei.haapi.service.MpbsService;
import school.hei.haapi.service.MpbsVerificationService;

@RestController
@AllArgsConstructor
public class MpbsController {
  private final CreateMpbsValidator validator;
  private final MpbsService mpbsService;
  private final MpbsMapper mapper;
  private final MpbsVerificationService mpbsVerificationService;

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

  @PostMapping(value = "/mpbs/verify", consumes = MULTIPART_FORM_DATA_VALUE)
  public List<Mpbs> verifyMpbs(@RequestPart(name = "file_to_upload") MultipartFile file)
      throws IOException {
    return mpbsVerificationService.computeFromXls(file).stream().map(mapper::toRest).toList();
  }
}
