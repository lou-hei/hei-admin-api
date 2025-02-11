package school.hei.haapi.service;

import static java.util.UUID.randomUUID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.*;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.REMEDIAL_COSTS;
import static school.hei.haapi.endpoint.rest.model.FeeTypeEnum.TUITION;
import static school.hei.haapi.endpoint.rest.model.PaymentFrequency.MONTHLY;
import static school.hei.haapi.endpoint.rest.model.PaymentFrequency.YEARLY;
import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static school.hei.haapi.service.FeeService.PaymentType.BANK;
import static school.hei.haapi.service.FeeService.PaymentType.MPBS;
import static school.hei.haapi.service.FeeService.StudentGrade.L1;
import static school.hei.haapi.service.FeeService.StudentGrade.L2;
import static school.hei.haapi.service.FeeService.StudentGrade.L3;
import static school.hei.haapi.service.utils.InstantUtils.getFirstDayOfActualMonth;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.LateFeeVerified;
import school.hei.haapi.endpoint.event.model.PojaEvent;
import school.hei.haapi.endpoint.event.model.StudentsWithOverdueFeesReminder;
import school.hei.haapi.endpoint.event.model.UnpaidFeesReminder;
import school.hei.haapi.endpoint.rest.model.AdvancedFeesStatistics;
import school.hei.haapi.endpoint.rest.model.FeeStatusEnum;
import school.hei.haapi.endpoint.rest.model.FeeTypeEnum;
import school.hei.haapi.endpoint.rest.model.FeesStatistics;
import school.hei.haapi.endpoint.rest.model.LateFeesStats;
import school.hei.haapi.endpoint.rest.model.MpbsStatus;
import school.hei.haapi.endpoint.rest.model.PaidFeesStats;
import school.hei.haapi.endpoint.rest.model.PaymentFrequency;
import school.hei.haapi.endpoint.rest.model.PendingFeesStats;
import school.hei.haapi.endpoint.rest.model.TotalExpectedFeesStats;
import school.hei.haapi.model.*;
import school.hei.haapi.model.exception.ApiException;
import school.hei.haapi.model.validator.FeeValidator;
import school.hei.haapi.model.validator.UpdateFeeValidator;
import school.hei.haapi.repository.FeeRepository;
import school.hei.haapi.repository.dao.FeeDao;
import school.hei.haapi.repository.model.FeesStats;
import school.hei.haapi.service.utils.DateUtils;
import school.hei.haapi.service.utils.XlsxCellsGenerator;

@Service
@AllArgsConstructor
@Slf4j
public class FeeService {
  private static final FeeStatusEnum DEFAULT_STATUS = LATE;
  private final FeeRepository feeRepository;
  private final FeeValidator feeValidator;
  private final UpdateFeeValidator updateFeeValidator;
  private final EventProducer<PojaEvent> eventProducer;
  private final FeeDao feeDao;
  private final FeeTemplateService feeTemplateService;
  private final DateUtils dateUtils;
  private static final String MONTHLY_FEE_TEMPLATE_NAME = "Frais mensuel L1";
  private static final String YEARLY_FEE_TEMPLATE_NAME = "Frais annuel L1";

  public byte[] generateFeesAsXlsx(FeeStatusEnum feeStatus, Instant from, Instant to) {
    XlsxCellsGenerator<Fee> xlsxCellsGenerator = new XlsxCellsGenerator<>();
    List<Fee> feeList = feeDao.findAllByStatusAndDueDatetimeBetween(feeStatus, from, to);
    return xlsxCellsGenerator.apply(
        feeList,
        List.of(
            "student.ref",
            "student.firstName",
            "student.lastName",
            "student.email",
            "totalAmount",
            "remainingAmount",
            "comment",
            "dueDatetime"));
  }

  public Fee debitAmountFromMpbs(Fee toUpdate, int amountToDebit) {
    int remainingAmount = toUpdate.getRemainingAmount();
    log.info("actual remaining amount before computing = {}", remainingAmount);
    if (remainingAmount == 0) {
      throw new ApiException(SERVER_EXCEPTION, "Remaining amount is already 0");
    }
    toUpdate.setRemainingAmount(remainingAmount - amountToDebit);

    int actualRemainingAmount = toUpdate.getRemainingAmount();
    log.info("actual remaining amount = {}", actualRemainingAmount);
    if (actualRemainingAmount <= 0) {
      log.info("if student paid over than expected = {}", actualRemainingAmount);
      toUpdate.setRemainingAmount(0);
      log.info(
          "set remaining amount even if student paid more = {}", toUpdate.getRemainingAmount());
    }
    return updateFeeStatus(toUpdate);
  }

  public Fee debitAmount(Fee toUpdate, int amountToDebit) {
    int remainingAmount = toUpdate.getRemainingAmount();

    if (remainingAmount == 0) {
      throw new ApiException(SERVER_EXCEPTION, "Remaining amount is already 0");
    }
    if (amountToDebit > remainingAmount) {
      throw new ApiException(SERVER_EXCEPTION, "Remaining amount is inferior to your request");
    }
    toUpdate.setRemainingAmount(remainingAmount - amountToDebit);
    return updateFeeStatus(toUpdate);
  }

  public Fee deleteFeeByStudentIdAndFeeId(String studentId, String feeId) {
    Fee deletedFee = getByStudentIdAndFeeId(studentId, feeId);
    feeRepository.deleteById(feeId);
    return deletedFee;
  }

  public Fee getById(String id) {
    var loggedFee = updateFeeStatus(feeRepository.findById(id).orElse(null));
    log.debug("fee: ------------########## {}", loggedFee);
    log.debug("now: ---------------######### {}", Instant.now());
    return loggedFee;
  }

  public Fee getByStudentIdAndFeeId(String studentId, String feeId) {
    return updateFeeStatus(feeRepository.getByStudentIdAndId(studentId, feeId));
  }

  @Transactional
  public List<Fee> saveAll(List<Fee> fees) {
    feeValidator.accept(fees);
    return feeRepository.saveAll(fees);
  }

  @Transactional
  public List<Fee> updateAll(List<Fee> fees, String studentId) {
    updateFeeValidator.accept(fees);
    return feeRepository.saveAll(fees);
  }

  public FeesStats getFeesStats(
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
      FeeStatusEnum status,
      Instant monthFrom,
      Instant monthTo,
      boolean isMpbs,
      String studentRef) {

    if (Objects.isNull(monthFrom)) monthFrom = getFirstDayOfActualMonth();

    var stats =
        feeDao.getStatByCriteria(
            mpbsStatus, feeType, status, studentRef, monthFrom, monthTo, isMpbs);
    return getHandledNullDataStats(stats);
  }

  public List<Fee> getFees(
      PageFromOne page,
      BoundedPageSize pageSize,
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
      FeeStatusEnum status,
      Instant monthFrom,
      Instant monthTo,
      boolean isMpbs,
      String studentRef) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());

    if (Objects.isNull(monthFrom)) monthFrom = getFirstDayOfActualMonth();

    return feeDao.getByCriteria(
        mpbsStatus, feeType, status, studentRef, monthFrom, monthTo, isMpbs, pageable);
  }

  public FeesStatistics getFeesStats(Instant monthFrom, Instant monthTo) {
    var result = feeDao.getStatByCriteria(null, null, null, null, monthFrom, monthTo, false);
    return FeesStats.to(getHandledNullDataStats(result));
  }

  public AdvancedFeesStatistics getAdvancedFeesStats(Instant monthFrom, Instant monthTo) {
    var instantRange = new DateUtils.RangedInstant(monthFrom, monthTo);
    List<Fee> allFees =
        feeRepository.findAllByDueDatetimeBetween(instantRange.from(), instantRange.to());
    return new AdvancedFeesStatistics()
        .lateFeesCount(getLateFeesStats(allFees))
        .paidFeesCount(getPaidFeesStats(allFees))
        .pendingFeesCount(getPendingFeesStats(allFees))
        .totalExpectedFeesCount(getTotalExpectedFeesStats(allFees));
  }

  private LateFeesStats getLateFeesStats(List<Fee> fees) {
    List<Fee> lateFees = filterFeesByStatus(fees, LATE);
    HashMap<StudentGrade, Long> feeCountByGrade = countFeesByGrades(lateFees);
    return new LateFeesStats()
        .remedialFeesCount(BigDecimal.valueOf(countRemedialFees(lateFees)))
        .workStudy(countWorkStudyFees(lateFees))
        .monthly(countFeesByPaymentFrequency(lateFees, MONTHLY))
        .yearly(countFeesByPaymentFrequency(lateFees, YEARLY))
        .firstGrade(feeCountByGrade.get(L1))
        .secondGrade(feeCountByGrade.get(L2))
        .thirdGrade(feeCountByGrade.get(L3));
  }

  private PaidFeesStats getPaidFeesStats(List<Fee> fees) {
    List<Fee> paidFees = filterFeesByStatus(fees, PAID);
    HashMap<StudentGrade, Long> feeCountByGrade = countFeesByGrades(paidFees);
    return new PaidFeesStats()
        .remedialFeesCount(BigDecimal.valueOf(countRemedialFees(paidFees)))
        .workStudy(countWorkStudyFees(paidFees))
        .monthly(countFeesByPaymentFrequency(paidFees, MONTHLY))
        .yearly(countFeesByPaymentFrequency(paidFees, YEARLY))
        .firstGrade(feeCountByGrade.get(L1))
        .secondGrade(feeCountByGrade.get(L2))
        .thirdGrade(feeCountByGrade.get(L3))
        .bankFees(BigDecimal.valueOf(countFeesByPaymentType(paidFees, BANK)))
        .mobileMoney(BigDecimal.valueOf(countFeesByPaymentType(paidFees, MPBS)));
  }

  private PendingFeesStats getPendingFeesStats(List<Fee> fees) {
    List<Fee> pendingFees = filterFeesByStatus(fees, PENDING);
    HashMap<StudentGrade, Long> feeCountByGrade = countFeesByGrades(pendingFees);
    return new PendingFeesStats()
        .remedialFeesCount(BigDecimal.valueOf(countRemedialFees(pendingFees)))
        .workStudy(countWorkStudyFees(pendingFees))
        .monthly(countFeesByPaymentFrequency(pendingFees, MONTHLY))
        .yearly(countFeesByPaymentFrequency(pendingFees, YEARLY))
        .firstGrade(feeCountByGrade.get(L1))
        .secondGrade(feeCountByGrade.get(L2))
        .thirdGrade(feeCountByGrade.get(L3));
  }

  private TotalExpectedFeesStats getTotalExpectedFeesStats(List<Fee> fees) {
    HashMap<StudentGrade, Long> feeCountByGrade = countFeesByGrades(fees);
    return new TotalExpectedFeesStats()
        .firstGrade(feeCountByGrade.get(L1))
        .secondGrade(feeCountByGrade.get(L2))
        .thirdGrade(feeCountByGrade.get(L3))
        .monthly(countFeesByPaymentFrequency(fees, MONTHLY))
        .yearly(countFeesByPaymentFrequency(fees, YEARLY))
        .workStudy(countWorkStudyFees(fees));
  }

  enum StudentGrade {
    L1,
    L2,
    L3
  }

  enum PaymentType {
    MPBS,
    BANK
  }

  private HashMap<StudentGrade, Long> countFeesByGrades(List<Fee> fees) {
    var feesByGrades = new HashMap<StudentGrade, Long>();
    long firstGradeCount =
        filterFeesByType(fees, TUITION).stream()
            .filter(fee -> (fee.getComment().toLowerCase().contains("l1")))
            .count();
    long secondGradeCount =
        filterFeesByType(fees, TUITION).stream()
            .filter(fee -> (fee.getComment().toLowerCase().contains("l2")))
            .count();
    long thirdGradeCount =
        filterFeesByType(fees, TUITION).stream()
            .filter(fee -> (fee.getComment().toLowerCase().contains("l3")))
            .count();

    feesByGrades.put(L1, firstGradeCount);
    feesByGrades.put(L2, secondGradeCount);
    feesByGrades.put(L3, thirdGradeCount);

    return feesByGrades;
  }

  private long countFeesByPaymentFrequency(List<Fee> fees, PaymentFrequency paymentFrequency) {
    return filterFeesByType(fees, TUITION).stream()
        .filter(
            fee -> {
              String feeComment = fee.getComment();
              return switch (paymentFrequency) {
                case MONTHLY -> feeComment.toLowerCase().contains("mensuel");
                case YEARLY -> feeComment.toLowerCase().contains("annuel");
              };
            })
        .count();
  }

  private long countFeesByPaymentType(List<Fee> fees, PaymentType paymentType) {
    return filterFeesByType(fees, TUITION).stream()
        .filter(
            fee ->
                switch (paymentType) {
                  case MPBS -> fee.getMpbs() != null;
                  case BANK -> fee.getMpbs() == null;
                })
        .count();
  }

  private long countRemedialFees(List<Fee> fees) {
    return fees.stream().filter(fee -> fee.getType().equals(REMEDIAL_COSTS)).count();
  }

  private long countWorkStudyFees(List<Fee> fees) {
    return filterFeesByType(fees, TUITION).stream()
        .filter(fee -> fee.getComment().toLowerCase().contains("alternance"))
        .count();
  }

  private List<Fee> filterFeesByType(List<Fee> fees, FeeTypeEnum feeType) {
    return fees.stream().filter(fee -> fee.getType().equals(feeType)).toList();
  }

  private List<Fee> filterFeesByStatus(List<Fee> fees, FeeStatusEnum feeStatus) {
    return fees.stream().filter(fee -> fee.getStatus().equals(feeStatus)).toList();
  }

  private FeesStats getHandledNullDataStats(List<FeesStats> feesStats) {
    if (feesStats.isEmpty()) {
      return FeesStats.builder()
          .totalYearlyFees(0L)
          .totalMonthlyFees(0L)
          .totalFees(0L)
          .countOfPendingTransaction(0L)
          .countOfSuccessTransaction(0L)
          .totalLateFees(0L)
          .totalUnpaidFees(0L)
          .totalPaidFees(0L)
          .build();
    }
    return feesStats.getFirst();
  }

  private int toInt(Object value) {
    return value instanceof Number ? ((Number) value).intValue() : 0;
  }

  public List<Fee> getFeesByStudentId(
      String studentId, PageFromOne page, BoundedPageSize pageSize, FeeStatusEnum status) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    if (status != null) {
      return feeRepository.getFeesByStudentIdAndStatus(studentId, status, pageable);
    }
    return feeRepository.findAllByStudentIdSortByStatusAndDueDatetimeDescAndId(studentId, pageable);
  }

  private Fee updateFeeStatus(Fee initialFee) {
    if (initialFee.getRemainingAmount() == 0) {
      initialFee.setStatus(PAID);
    } else if (Instant.now().isAfter(initialFee.getDueDatetime())
        && initialFee.getStatus() == UNPAID) {
      initialFee.setStatus(LATE);
    }
    return feeRepository.save(initialFee);
  }

  @Transactional
  public List<Fee> saveFromPaymentFrequency(
      User user, PaymentFrequency frequency, Instant firstDueDatetime) {

    List<Fee> feesToSave =
        switch (frequency) {
          case MONTHLY ->
              createFeesFromFeeTemplate(MONTHLY_FEE_TEMPLATE_NAME, user, firstDueDatetime);
          case YEARLY ->
              createFeesFromFeeTemplate(YEARLY_FEE_TEMPLATE_NAME, user, firstDueDatetime);
        };
    return feeRepository.saveAll(feesToSave);
  }

  public List<Fee> createFeesFromFeeTemplate(String feeTemplateName, User user, Instant instant) {
    FeeTemplate feeTemplate = feeTemplateService.getFeeTemplateByName(feeTemplateName);
    List<Fee> fees = new ArrayList<>();
    for (int i = 0; i < feeTemplate.getNumberOfPayments(); i++) {
      Fee fee =
          Fee.builder()
              .id(randomUUID().toString())
              .comment(feeTemplate.getName())
              .totalAmount(feeTemplate.getAmount())
              .remainingAmount(feeTemplate.getAmount())
              .student(user)
              .creationDatetime(Instant.now())
              .status(UNPAID)
              .updatedAt(Instant.now())
              .dueDatetime(getDueDatetime(i, instant))
              .isDeleted(false)
              .type(TUITION)
              .build();
      fees.add(fee);
    }
    return fees;
  }

  public Instant getDueDatetime(Integer monthToAdd, Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneId.of("UTC+3"))
        .plusMonths(monthToAdd)
        .atZone(ZoneId.of("UTC+3"))
        .toInstant();
  }

  @Transactional
  public List<Fee> updateFeesStatusToLate() {
    Instant now = Instant.now();
    List<Fee> unpaidFees = feeRepository.getUnpaidFees(now);
    var lateFees = new ArrayList<Fee>();
    unpaidFees.forEach(
        fee -> {
          var modifiedFee = updateFeeStatus(fee);
          log.info(
              "Fee "
                  + modifiedFee.describe()
                  + "with id."
                  + fee.getId()
                  + " is going to be updated from UNPAID to "
                  + fee.getStatus());
          /*if (PAID.equals(modifiedFee.getStatus())) {
            paidFees.add(modifiedFee);
          } else*/
          if (LATE.equals(modifiedFee.getStatus())) {
            lateFees.add(modifiedFee);
          }
        });
    lateFees.forEach(lf -> feeRepository.updateFeeStatusById(LATE, lf.getId()));
    log.info("lateFees = {}", lateFees.stream().map(Fee::describe).toList());
    // Send list of late fees with student ref to contact
    if (!lateFees.isEmpty()) {
      eventProducer.accept(List.of(toStudentsWithOverdueFeesReminder(lateFees)));
    }
    return lateFees;
  }

  @Transactional
  public LateFeeVerified toLateFeeEvent(Fee fee) {
    return LateFeeVerified.builder()
        .type(fee.getType())
        .student(LateFeeVerified.FeeUser.from(fee.getStudent()))
        .comment(fee.getComment())
        .remainingAmount(fee.getRemainingAmount())
        .dueDatetime(fee.getDueDatetime())
        .build();
  }

  public UnpaidFeesReminder toUnpaidFeesReminder(Fee fee) {
    return UnpaidFeesReminder.builder()
        .user(UnpaidFeesReminder.UnpaidFeesUser.from(fee.getStudent()))
        .remainingAmount(fee.getRemainingAmount())
        .dueDatetime(fee.getDueDatetime())
        .build();
  }

  public StudentsWithOverdueFeesReminder toStudentsWithOverdueFeesReminder(List<Fee> fees) {
    return StudentsWithOverdueFeesReminder.builder()
        .id(String.valueOf(randomUUID()))
        .students(
            fees.stream()
                .map(StudentsWithOverdueFeesReminder.StudentWithOverdueFees::from)
                .toList())
        .build();
  }

  @Transactional
  public void sendLateFeesEmail() {
    List<Fee> lateFees = feeRepository.findAllByStatus(LATE);
    log.info("Late fees size: " + lateFees.size());
    lateFees.forEach(
        fee -> {
          eventProducer.accept(List.of(toLateFeeEvent(fee)));
          log.info("Late Fee with id." + fee.getId() + " is sent to Queue");
        });
  }

  public void sendUnpaidFeesEmail() {
    List<Fee> unpaidFees =
        feeRepository.getUnpaidFeesForTheMonthSpecified(
            Instant.now().atZone(ZoneId.of("UTC+3")).getMonthValue());
    log.info("Unpaid fees size: {}", unpaidFees.size());
    unpaidFees.forEach(
        unpaidFee -> {
          eventProducer.accept(List.of(toUnpaidFeesReminder(unpaidFee)));
          log.info("Unpaid fee with id.{} is sent to Queue", unpaidFee.getId());
        });
  }

  public Fee update(Fee fee) {
    return feeRepository.save(fee);
  }
}
