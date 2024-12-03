package school.hei.haapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.UNPAID;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.HARDWARE;
import static school.hei.haapi.endpoint.rest.model.Payment.TypeEnum.CASH;
import static school.hei.haapi.integration.conf.TestUtils.FEE1_ID;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.integration.conf.TestUtils;
import school.hei.haapi.model.*;
import school.hei.haapi.model.validator.FeeValidator;
import school.hei.haapi.model.validator.UpdateFeeValidator;
import school.hei.haapi.repository.FeeRepository;
import school.hei.haapi.repository.dao.FeeDao;
import school.hei.haapi.service.utils.DateUtils;

class FeeServiceTest {
  FeeService subject;
  FeeRepository feeRepository;
  FeeValidator feeValidator;
  EventProducer eventProducer;
  UpdateFeeValidator updateFeeValidator;
  UserService userService;
  FeeDao feeDao;
  FeeTemplateService feeTemplateService;
  DateUtils dateUtils;

  static User student1() {
    return User.builder().id(TestUtils.STUDENT1_ID).build();
  }

  static int remainingAmount() {
    return 4000;
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

  static Fee fee(int paymentAmount) {
    Instant today = Instant.now();
    Instant tomorrow = today.plus(1, ChronoUnit.DAYS);
    return createSomeFee(FEE1_ID, paymentAmount, UNPAID, tomorrow, today);
  }

  static Fee createMockedFee(
      boolean isMocked,
      String feeId,
      int paymentAmount,
      int remainingAmount,
      school.hei.haapi.endpoint.rest.model.FeeStatusEnum status) {
    Instant dueDatetime = Instant.parse("2022-01-02T00:00:00.00Z");
    Instant creationDatetime = Instant.parse("2022-01-01T00:00:00.00Z");
    Fee fee = createSomeFee(feeId, paymentAmount, status, dueDatetime, creationDatetime);
    fee.setRemainingAmount(remainingAmount);
    if (isMocked) {
      fee.setStatus(UNPAID);
      fee.setRemainingAmount(remainingAmount());
    }
    return fee;
  }

  static Fee fee1(boolean isMocked) {
    return createMockedFee(isMocked, FEE1_ID, remainingAmount(), 0, PAID);
  }

  static Fee fee2(boolean isMocked) {
    return createMockedFee(isMocked, TestUtils.FEE2_ID, remainingAmount(), 0, PAID);
  }

  static Fee fee3(boolean isMocked) {
    int rest = 1;
    return createMockedFee(isMocked, FEE1_ID, remainingAmount() - rest, rest, LATE);
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

  @BeforeEach
  void setUp() {
    feeRepository = mock(FeeRepository.class);
    feeValidator = mock(FeeValidator.class);
    updateFeeValidator = mock(UpdateFeeValidator.class);
    eventProducer = mock(EventProducer.class);
    userService = mock(UserService.class);
    feeDao = mock(FeeDao.class);
    feeTemplateService = mock(FeeTemplateService.class);
    subject =
        new FeeService(
            feeRepository,
            feeValidator,
            updateFeeValidator,
            eventProducer,
            feeDao,
            feeTemplateService,
            dateUtils);
  }

  @Test
  void fee_status_is_paid_with_overpaid_mpbs() {
    Fee initial = fee(0);
    when(feeRepository.save(any(Fee.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    Fee actual = subject.debitAmountFromMpbs(initial, 5000);

    assertEquals(PAID, actual.getStatus());
    assertEquals(0, actual.getRemainingAmount());
  }

  @Test
  void fee_status_is_paid() {
    Fee initial = fee(remainingAmount());
    when(feeRepository.save(any(Fee.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(feeRepository.getById(FEE1_ID))
        .thenReturn(initial.toBuilder().remainingAmount(0).status(PAID).build());

    Fee actual = subject.getById(FEE1_ID);

    assertEquals(UNPAID, initial.getStatus());
    assertEquals(remainingAmount(), initial.getRemainingAmount());
    assertEquals(PAID, actual.getStatus());
    assertEquals(0, actual.getRemainingAmount());
  }

  @Test
  void fee_status_is_unpaid() {
    int rest = 1000;
    int paymentAmount = remainingAmount() - rest;
    Fee initial = fee(paymentAmount);
    when(feeRepository.save(any(Fee.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(feeRepository.getById(FEE1_ID))
        .thenReturn(
            initial.toBuilder()
                .remainingAmount(remainingAmount() - paymentAmount)
                .status(UNPAID)
                .build());

    Fee actual = subject.getById(FEE1_ID);

    assertEquals(UNPAID, actual.getStatus());
    assertEquals(rest, actual.getRemainingAmount());
    assertTrue(actual.getDueDatetime().isAfter(Instant.now()));
  }

  @Test
  void fee_status_is_late() {
    int rest = 1000;
    int paymentAmount = remainingAmount() - rest;
    Fee initial = fee(paymentAmount);
    Instant yesterday = Instant.now().minus(1L, ChronoUnit.DAYS);
    initial.setDueDatetime(yesterday);
    when(feeRepository.save(any(Fee.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    when(feeRepository.getById(FEE1_ID))
        .thenReturn(
            initial.toBuilder()
                .remainingAmount(remainingAmount() - paymentAmount)
                .status(LATE)
                .build());

    Fee actual = subject.getById(FEE1_ID);

    assertEquals(LATE, actual.getStatus());
    assertEquals(rest, actual.getRemainingAmount());
    assertTrue(actual.getDueDatetime().isBefore(Instant.now()));
  }

  @Test
  void fees_by_status_with_exceeded_page() {
    PageFromOne page1 = new PageFromOne(1);
    PageFromOne page2 = new PageFromOne(2);
    BoundedPageSize pageSize = new BoundedPageSize(10);
    boolean isMocked = true;
    when(feeRepository.findAll())
        .thenReturn(List.of(fee1(isMocked), fee2(isMocked), fee3(isMocked)));

    List<Fee> actualPaidPage1 = subject.getFees(page1, pageSize, null, null, PAID, null, null, false, null);
    List<Fee> actualLatePage1 = subject.getFees(page1, pageSize, null, null, LATE, null, null, false, null);
    List<Fee> actualLatePage2 = subject.getFees(page2, pageSize, null, null, LATE, null, null, false, null);

    assertEquals(0, actualPaidPage1.size());
    assertEquals(0, actualLatePage1.size());
    assertEquals(0, actualLatePage2.size());
    assertFalse(actualPaidPage1.contains(fee1(!isMocked)));
    assertFalse(actualPaidPage1.contains(fee2(!isMocked)));
    assertFalse(actualLatePage1.contains(fee3(!isMocked)));
  }
}
