package school.hei.haapi.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.repository.DelayPenaltyRepository;

@Service
@AllArgsConstructor
public class DelayPenaltyService {
  private final DelayPenaltyRepository repository;

  public DelayPenalty getOneOrderByCreationDatetimeDesc() {
    return repository.findFirstByOrderByCreationDatetimeDesc();
  }

  public DelayPenalty save(DelayPenalty data) {
    return repository.save(data);
  }
}
