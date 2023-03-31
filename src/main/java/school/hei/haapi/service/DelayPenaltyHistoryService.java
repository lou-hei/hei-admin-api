package school.hei.haapi.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.DelayPenalty;
import school.hei.haapi.model.DelayPenaltyHistory;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.repository.DelayPenaltyHistoryRepository;

@Service
@AllArgsConstructor
public class DelayPenaltyHistoryService {
  private final DelayPenaltyHistoryRepository repository;
  private final DelayPenaltyHistoryRepository delayPenaltyHistoryRepository;
  private final DelayPenaltyService delayPenaltyService;

  public DelayPenaltyHistory getById(String delayHistory) {
    return repository.getById(delayHistory);
  }

  public DelayPenaltyHistory save(DelayPenaltyHistory delayPenaltyHistory) {
    return repository.save(delayPenaltyHistory);
  }

  public List<DelayPenaltyHistory> findDelayPenaltyHistoriesByInterestStartAndEnd(
      LocalDate InterestStart, LocalDate InterestEnd) {
    return repository.findDelayPenaltyHistoriesByInterestStartAndEnd(InterestStart, InterestEnd);
  }

  public DelayPenaltyHistory toBeSaved(DelayPenalty delayPenalty) {
    DelayPenaltyHistory previousDelayPenalty = getBeforeLastItem();
    DelayPenaltyHistory lastDelayPenalty = getLastItem();
    return DelayPenaltyHistory.builder()
        .delayPenalty(delayPenalty)
        .interestPercent(delayPenalty.getInterestPercent())
        .timeFrequency(dayFrequency(Objects.requireNonNull(delayPenalty.getInterestTimeRate())))
        .startDate(previousDelayPenalty.getEndDate())
        .endDate(null)
        .creationDate(Instant.now()).build();
  }

  public DelayPenaltyHistory toSavePrevious(DelayPenalty delayPenalty) {
    DelayPenaltyHistory previousDelayPenalty = getBeforeLastItem();
    DelayPenaltyHistory lastDelayPenalty = getLastItem();
    Instant instant = Instant.now();
    ZoneId zoneId = ZoneId.systemDefault();
    ZonedDateTime zonedDateTime = instant.atZone(zoneId);
    LocalDate localDate = zonedDateTime.toLocalDate();
    return DelayPenaltyHistory.builder()
        .id(lastDelayPenalty.getId())
        .delayPenalty(delayPenalty)
        .interestPercent(lastDelayPenalty.getInterestPercent())
        .timeFrequency(dayFrequency(Objects.requireNonNull(delayPenalty.getInterestTimeRate())))
        .startDate(lastDelayPenalty.getStartDate())
        .endDate(localDate)
        .creationDate(lastDelayPenalty.getCreationDate()).build();
  }

  private DelayPenaltyHistory getLastItem() {
    int indexOfLastItem = delayPenaltyHistoryRepository.findAll().size() - 1;
    return delayPenaltyHistoryRepository.findAll().get(indexOfLastItem);
  }

  private DelayPenaltyHistory getBeforeLastItem() {
    int indexOfLastItem = delayPenaltyHistoryRepository.findAll().size() - 2;
    return delayPenaltyHistoryRepository.findAll().get(indexOfLastItem);
  }

  public int dayFrequency(
      school.hei.haapi.endpoint.rest.model.DelayPenalty.InterestTimerateEnum interestTimeRate) {
    switch (interestTimeRate) {
      case DAILY:
        return 1;
      default:
        throw new BadRequestException(
            "Unexpected delay Penalty Interest Time rate: " + interestTimeRate.getValue());
    }
  }
}
