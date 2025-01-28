package school.hei.haapi.service;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.LATE;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.MISSING;
import static school.hei.haapi.endpoint.rest.model.AttendanceStatus.PRESENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.MissedEventEmail;
import school.hei.haapi.endpoint.rest.model.EventStats;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.EventParticipant;
import school.hei.haapi.model.Group;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.EventParticipantRepository;
import school.hei.haapi.repository.dao.EventDao;

@Service
@AllArgsConstructor
public class EventParticipantService {

  private final EventParticipantRepository eventParticipantRepository;
  private final UserService userService;
  private final EventProducer<MissedEventEmail> eventProducer;
  private final GroupService groupService;
  private final EventDao eventDao;

  public List<EventParticipant> getEventParticipants(
      String eventId, PageFromOne page, BoundedPageSize pageSize, String groupRef) {

    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(ASC, "participant.ref"));

    return Objects.isNull(groupRef)
        ? findByEventId(eventId, pageable)
        : findByEventIdAndGroupRef(eventId, groupRef, pageable);
  }

  public List<EventParticipant> updateEventParticipants(List<EventParticipant> eventParticipants) {
    return eventParticipantRepository.saveAll(eventParticipants);
  }

  public void createEventParticipantsForAGroup(Group group, Event event) {
    String groupId = group.getId();
    String eventId = event.getId();
    List<User> users = userService.getByGroupId(groupId);
    List<EventParticipant> eventParticipants = new ArrayList<>();
    Group actualGroup = groupService.findById(groupId);
    users.forEach(
        user -> {
          if (!isParticipantAlreadyInEvent(eventId, groupId, user.getId())) {
            EventParticipant newEventParticipant =
                EventParticipant.builder()
                    .participant(user)
                    .group(actualGroup)
                    .event(event)
                    .status(MISSING)
                    .build();
            eventParticipants.add(newEventParticipant);
          } else {
            // nothing
          }
        });
    eventParticipantRepository.saveAll(eventParticipants);
  }

  public EventParticipant findById(String id) {
    return eventParticipantRepository
        .findById(id)
        .orElseThrow(
            () -> new NotFoundException("Event participant with id #" + id + "does not exist"));
  }

  public List<EventParticipant> findByEventId(String eventId, Pageable pageable) {
    return eventParticipantRepository
        .findAllByEventId(eventId, pageable)
        .orElseThrow(() -> new NotFoundException("Event with id #" + eventId + " does not exist"));
  }

  public List<EventParticipant> findByEventIdAndGroupRef(
      String eventId, String groupRef, Pageable pageable) {
    return eventParticipantRepository
        .findAllByEventIdAndGroupRef(eventId, groupRef, pageable)
        .orElseThrow(() -> new NotFoundException("Event with id #" + eventId + " does not exist"));
  }

  public EventStats getEventParticipantsStats(String eventId) {
    Integer missing = eventParticipantRepository.countByEventIdAndStatus(eventId, MISSING);
    Integer late = eventParticipantRepository.countByEventIdAndStatus(eventId, LATE);
    Integer present = eventParticipantRepository.countByEventIdAndStatus(eventId, PRESENT);

    return new EventStats()
        .late(late)
        .missing(missing)
        .present(present)
        .total(missing + present + late);
  }

  private boolean isParticipantAlreadyInEvent(String eventId, String groupId, String userId) {
    return eventParticipantRepository.existsByEventIdAndGroupIdAndParticipantId(
        eventId, groupId, userId);
  }
}
