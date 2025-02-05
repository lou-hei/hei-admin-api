package school.hei.haapi.endpoint.rest.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.CorMapper;
import school.hei.haapi.endpoint.rest.model.Cor;
import school.hei.haapi.endpoint.rest.model.CrupdateCor;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.CorService;

@RestController
@RequiredArgsConstructor
public class CorController {
  private final CorService corService;
  private final CorMapper corMapper;

  @GetMapping("/students/{student_id}/cors")
  public List<Cor> getStudentCors(
      @PathVariable(name = "student_id") String studentId,
      @RequestParam(name = "page", required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(name = "page_size", required = false, defaultValue = "15")
          BoundedPageSize pageSize) {
    return corMapper.toRestList(corService.getCorByStudentId(studentId, page, pageSize));
  }

  @PutMapping("/students/{student_id}/cors")
  public List<Cor> updateStudentCors(
      @PathVariable(name = "student_id") String studentId, @RequestBody List<CrupdateCor> cors) {
    return corMapper.toRestList(
        corService.savaAllStudentCor(corMapper.toDomainList(cors, studentId)));
  }
}
