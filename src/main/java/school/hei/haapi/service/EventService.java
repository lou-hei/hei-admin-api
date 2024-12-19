package school.hei.haapi.service;

import static org.springframework.data.domain.Sort.Direction.DESC;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.rest.model.EventType;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.EventRepository;
import school.hei.haapi.repository.dao.EventDao;

@Service
@AllArgsConstructor
public class EventService {
  private final EventRepository eventRepository;
  private final EventDao eventDao;
  private final EventParticipantService eventParticipantService;

  public List<Event> createOrUpdateEvent(List<Event> eventToCrupdate) {
    List<Event> eventsCreated = eventRepository.saveAll(eventToCrupdate);
    for (Event event : eventsCreated) {
      event
          .getGroups()
          .forEach(group -> eventParticipantService.createEventParticipantsForAGroup(group, event));
    }
    return eventsCreated;
  }

  public Event findEventById(String eventId) {
    return eventRepository
        .findById(eventId)
        .orElseThrow(
            () -> {
              throw new NotFoundException("Event with id #" + eventId + " not found");
            });
  }

  public List<Event> getEvents(
      Instant from,
      Instant to,
      String title,
      EventType eventType,
      Group group,
      PageFromOne page,
      BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(DESC, "beginDatetime"));
    return eventDao.findByCriteria(title, from, to, eventType, group, pageable);
  }
}
