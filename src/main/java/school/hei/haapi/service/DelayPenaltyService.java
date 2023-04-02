package school.hei.haapi.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.model.Fee;
import school.hei.haapi.repository.DelayPenaltyRepository;
import school.hei.haapi.repository.FeeRepository;
import school.hei.haapi.service.utils.FeePenaltyUtils;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class DelayPenaltyService {

    private final DelayPenaltyRepository delayPenaltyRepository;
    private final FeeRepository feeRepository;
    private final DelayPenalty defaultDelayPenalty = DelayPenalty.builder()
            .interestPercent(0)
            .interestTimeRate(school.hei.haapi.endpoint.rest.model.DelayPenalty.InterestTimeRateEnum.DAILY)
            .graceDelay(0)
            .applicabilityDelayAfterGrace(0)
            .build();

    public DelayPenalty getCurrentDelayPenalty() {
        List<DelayPenalty> delayPenalties = delayPenaltyRepository.findAll();
        if (delayPenalties.size() == 0) {
            return saveDelayPenalty(defaultDelayPenalty);
        }
        return delayPenalties.get(0);
    }

    public DelayPenalty saveDelayPenalty(DelayPenalty delayPenalty) {
        DelayPenalty newDelayPenalty = delayPenaltyRepository.save(delayPenalty);
        List<Fee> fees = feeRepository.getUnpaidAndLateFees();
        fees = FeePenaltyUtils.penalizeFees(fees, newDelayPenalty);
        feeRepository.saveAll(fees);
        return newDelayPenalty;
    }
}
