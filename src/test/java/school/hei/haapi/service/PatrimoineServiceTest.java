package school.hei.haapi.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.*;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.HARDWARE;
import static school.hei.haapi.endpoint.rest.model.Payment.TypeEnum.CASH;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;
import static school.hei.haapi.integration.conf.TestUtils.FEE3_ID;

import java.io.File;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Payment;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.FeeRepository;

class PatrimoineServiceTest {
  PatrimoineService subject;
  FeeRepository feeRepository;

  static User student1() {
    return User.builder().id(TestUtils.STUDENT1_ID).build();
  }

  static Payment payment1(int amount, Instant creationDatetime) {
    return Payment.builder()
        .id(TestUtils.PAYMENT1_ID)
        .type(CASH)
        .amount(amount)
        .comment(null)
        .creationDatetime(creationDatetime)
        .build();
  }

  static int remainingAmount() {
    return 300000;
  }

  static Instant dateTime() {
    return Instant.parse("2024-12-06T00:00:00.00Z");
  }

  static Fee createSomeFee(
      String feeId,
      int paymentAmount,
      school.hei.haapi.endpoint.rest.model.FeeStatusEnum status,
      Instant dueDatetime,
      Instant creationDatetime) {
    return Fee.builder()
        .id(feeId)
        .remainingAmount(remainingAmount())
        .totalAmount(remainingAmount())
        .type(HARDWARE)
        .comment(null)
        .dueDatetime(dueDatetime)
        .creationDatetime(creationDatetime)
        .status(status)
        .student(student1())
        .payments(List.of(payment1(paymentAmount, creationDatetime)))
        .build();
  }

  static Fee createMockedFee(
      boolean isMocked,
      String feeId,
      int paymentAmount,
      int remainingAmount,
      school.hei.haapi.endpoint.rest.model.FeeStatusEnum status) {
    Instant dueDatetime = Instant.parse("2025-01-06T00:00:00.00Z");
    Instant creationDatetime = dateTime();
    Fee fee = createSomeFee(feeId, paymentAmount, status, dueDatetime, creationDatetime);
    fee.setRemainingAmount(remainingAmount);
    if (isMocked) {
      fee.setStatus(UNPAID);
      fee.setRemainingAmount(remainingAmount());
    }
    return fee;
  }

  static Fee fee1(boolean isMocked) {
    return createMockedFee(isMocked, FEE1_ID, remainingAmount(), 0, UNPAID);
  }

  static Fee fee2(boolean isMocked) {
    return createMockedFee(isMocked, TestUtils.FEE2_ID, remainingAmount(), 0, UNPAID);
  }

  static Fee fee3(boolean isMocked) {
    return createMockedFee(isMocked, FEE3_ID, remainingAmount(), 0, UNPAID);
  }

  @BeforeEach
  void setUp() {
    feeRepository = mock(FeeRepository.class);
    subject = new PatrimoineService(feeRepository);
  }

  @Test
  void unpaid_fees_projection_ok() {
    Instant now = Instant.now();
    boolean isMocked = true;
    when(feeRepository.getUnpaidFees(now))
        .thenReturn(List.of(fee1(isMocked), fee2(isMocked), fee3(isMocked)));

    File unpaidFeesGraph = subject.visualizeUnpaidFees(dateTime());

    assertNotNull(unpaidFeesGraph);
    assertTrue(unpaidFeesGraph.exists());
  }
}
