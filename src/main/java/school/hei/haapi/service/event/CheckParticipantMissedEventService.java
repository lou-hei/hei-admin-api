package school.hei.haapi.service.event;

import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.MISSING;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.CheckParticipantMissedEventTriggered;
import school.hei.haapi.endpoint.event.model.MissedEventEmail;
import school.hei.haapi.model.Event;
import school.hei.haapi.repository.dao.EventDao;
import school.hei.haapi.service.EventParticipantService;

@Service
@AllArgsConstructor
public class CheckParticipantMissedEventService
    implements Consumer<CheckParticipantMissedEventTriggered> {
  private final EventDao eventDao;
  private final EventParticipantService eventParticipantService;
  private final EventProducer<MissedEventEmail> eventProducer;

  @Override
  public void accept(CheckParticipantMissedEventTriggered checkMissedEventServiceTriggered) {
    List<MissedEventEmail> missedEventEmails = new ArrayList<>();

    List<Event> todayEvents =
        eventDao.findByCriteria(
            null,
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).toInstant(ZoneOffset.ofHours(3)),
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).toInstant(ZoneOffset.ofHours(3)),
            null,
            null,
            null);

    todayEvents.forEach(
        event -> {
          List<MissedEventEmail> missingParticipants =
              eventParticipantService.findByEventId(event.getId(), null).stream()
                  .filter(eventParticipant -> MISSING.equals(eventParticipant.getStatus()))
                  .map(MissedEventEmail::fromEventParticipant)
                  .collect(Collectors.toUnmodifiableList());

          missedEventEmails.addAll(missingParticipants);
        });

    eventProducer.accept(missedEventEmails);
  }
}
