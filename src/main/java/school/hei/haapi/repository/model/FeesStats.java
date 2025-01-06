package school.hei.haapi.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import school.hei.haapi.endpoint.rest.model.FeesStatistics;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class FeesStats {
  private Long totalFees;
  private Long totalPaidFees;
  private Long totalUnpaidFees;
  private Long totalLateFees;
  private Long countOfPendingTransaction;
  private Long countOfSuccessTransaction;
  private Long totalMonthlyFees;
  private Long totalYearlyFees;

  public static FeesStatistics to(FeesStats feesStats) {
    return new FeesStatistics()
        .lateFees(
            feesStats.getTotalLateFees() == null ? 0 : feesStats.getTotalLateFees().intValue())
        .paidFees(
            feesStats.getTotalPaidFees() == null ? 0 : feesStats.getTotalPaidFees().intValue())
        .unpaidFees(
            feesStats.getTotalUnpaidFees() == null ? 0 : feesStats.getTotalUnpaidFees().intValue())
        .totalFees(feesStats.getTotalFees() == null ? 0 : feesStats.getTotalFees().intValue())
        .paidByTransaction(
            feesStats.getCountOfSuccessTransaction() == null
                ? 0
                : feesStats.getCountOfSuccessTransaction().intValue())
        .pendingTransaction(
            feesStats.getCountOfPendingTransaction() == null
                ? 0
                : feesStats.getCountOfPendingTransaction().intValue())
        .totalMonthlyFees(
            feesStats.getTotalMonthlyFees() == null
                ? 0
                : feesStats.getTotalMonthlyFees().intValue())
        .totalYearlyFees(
            feesStats.getTotalYearlyFees() == null ? 0 : feesStats.getTotalYearlyFees().intValue());
  }
}
