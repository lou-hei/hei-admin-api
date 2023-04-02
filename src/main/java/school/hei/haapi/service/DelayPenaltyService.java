package school.hei.haapi.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.repository.DelayPenaltyRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class DelayPenaltyService {

    private final DelayPenaltyRepository repository;

    public DelayPenalty getCurrentDelay(){
        return repository.findCurrentDelayPenalty().get(0);
    }
}
