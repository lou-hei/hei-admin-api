package school.hei.haapi.repository.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FeesStats {
    private Long totalFees;
    private Long totalPaidFees;
    private Long totalUnpaidFees;
    private Long totalLateFees;
    private Long countOfPendingTransaction;
    private Long countOfSuccessTransaction;
    private Long totalYearlyFees;
    private Long totalMonthlyFees;
}
