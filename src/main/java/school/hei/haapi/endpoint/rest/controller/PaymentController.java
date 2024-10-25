package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.PaymentMapper;
import school.hei.haapi.endpoint.rest.model.CreatePayment;
import school.hei.haapi.endpoint.rest.model.Payment;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.exception.ForbiddenException;
import school.hei.haapi.service.FeeService;
import school.hei.haapi.service.PaymentService;

@RestController
@AllArgsConstructor
public class PaymentController {
  private final PaymentService paymentService;
  private final PaymentMapper paymentMapper;
  private final FeeService feeService;

  @PostMapping("/students/{studentId}/fees/{feeId}/payments")
  public List<Payment> createPayments(
      @PathVariable String feeId,
      @RequestBody List<CreatePayment> toCreate,
      @PathVariable(name = "studentId") String studentId) {
    return paymentService.saveAll(paymentMapper.toDomainPayment(feeId, toCreate)).stream()
        .map(paymentMapper::toRestPayment)
        .collect(toUnmodifiableList());
  }

  @DeleteMapping("/students/{studentId}/fees/{feeId}/payments/{paymentId}")
  public Payment deleteStudentFeePaymentById(
      @PathVariable(name = "studentId") String studentId,
      @PathVariable(name = "feeId") String feeId,
      @PathVariable("paymentId") String paymentId) {
    return paymentMapper.toRestPayment(paymentService.deleteFeePaymentById(paymentId));
  }

  @GetMapping("/students/{studentId}/fees/{feeId}/payments")
  public List<Payment> getPaymentsByStudentId(
      @PathVariable String studentId,
      @PathVariable String feeId,
      @RequestParam PageFromOne page,
      @RequestParam("page_size") BoundedPageSize pageSize) {
    return paymentService.getByStudentIdAndFeeId(studentId, feeId, page, pageSize).stream()
        .map(paymentMapper::toRestPayment)
        .collect(toUnmodifiableList());
  }
}
