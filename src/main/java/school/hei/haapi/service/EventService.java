package school.hei.haapi.service;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.springframework.data.domain.Sort.Direction.DESC;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.rest.model.EventStats;
import school.hei.haapi.endpoint.rest.model.EventType;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.http.model.CreateEventFrequency;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.exception.BadRequestException;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.EventRepository;
import school.hei.haapi.repository.dao.EventDao;
import school.hei.haapi.service.utils.DateProducerFrom;
import school.hei.haapi.service.utils.DatetimeProducer;

@Service
@AllArgsConstructor
public class EventService {
  private final EventRepository eventRepository;
  private final EventDao eventDao;
  private final EventParticipantService eventParticipantService;
  private final DateProducerFrom dateProducerFrom;
  private final DatetimeProducer datetimeProducer;

  public List<Event> createOrUpdateEvent(
      List<Event> eventToCrupdate, CreateEventFrequency eventFrequencyToCreate) {
    List<Event> eventsWithFrequencies =
        generateEventFromFrequency(eventToCrupdate, eventFrequencyToCreate);
    List<Event> eventsCreated = eventRepository.saveAll(eventsWithFrequencies);

    for (Event event : eventsCreated) {
      event
          .getGroups()
          .forEach(group -> eventParticipantService.createEventParticipantsForAGroup(group, event));
    }
    return eventsCreated;
  }

  public EventStats getStats(String eventId, Instant from, Instant to) {
    Optional<String> optionalEventId = Optional.ofNullable(eventId);
    Optional<Instant> optionalFrom = Optional.ofNullable(from);
    Optional<Instant> optionalTo = Optional.ofNullable(to);
    if (optionalEventId.isPresent()) {
      String evId = optionalEventId.get();
      eventRepository
          .findById(evId)
          .orElseThrow(() -> new NotFoundException("Event with id : " + evId + "not found"));

      return eventParticipantService.getEventParticipantsStats(evId);
    }

    if (optionalFrom.isEmpty() && optionalTo.isEmpty()) {
      return eventParticipantService.getOverallEventParticipantsStats();
    }

    Instant fromInstant = optionalFrom.orElse(Instant.now());
    Instant toInstant = optionalTo.orElse(Instant.now());

    if (fromInstant.isAfter(toInstant)) {
      throw new BadRequestException("from cannot be after to");
    }

    List<Event> filteredEvents =
        eventDao.findByCriteria(null, fromInstant, toInstant, null, null, null);
    List<String> filteredEventIds =
        filteredEvents.stream().map(Event::getId).collect(toUnmodifiableList());
    return eventParticipantService.getEventParticipantsStats(filteredEventIds);
  }

  public Event findEventById(String eventId) {
    return eventRepository
        .findById(eventId)
        .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));
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

  private List<Event> generateEventFromFrequency(
      List<Event> eventToCrupdate, CreateEventFrequency eventFrequencyToCreate) {
    // TIPS: by default when we create event, the front send always only one event to be created
    // then:
    Instant originOfTheFrequencies = eventToCrupdate.getFirst().getBeginDatetime();
    Optional<CreateEventFrequency> optionalFrequency = generateFrequency(eventFrequencyToCreate);
    List<Event> eventsToSave = new ArrayList<>();

    if (optionalFrequency.isPresent()) {
      int goalsDay = eventFrequencyToCreate.getEventFrequencyNumber().getValue();
      List<LocalDate> eachDateOfEventFrequency =
          dateProducerFrom.apply(
              eventFrequencyToCreate.getFrequencyScopeDay(), goalsDay, originOfTheFrequencies);
      List<Instant> eachDatetimeBeginningOfEventFrequency =
          datetimeProducer.apply(
              eachDateOfEventFrequency, eventFrequencyToCreate.getFrequencyBeginningHour());
      List<Instant> eachDatetimeEndingOfEventFrequency =
          datetimeProducer.apply(
              eachDateOfEventFrequency, eventFrequencyToCreate.getFrequencyEndingHour());
      Map<Instant, Instant> eventSchedule =
          combineBeginningAndEndingHour(
              eachDatetimeBeginningOfEventFrequency, eachDatetimeEndingOfEventFrequency);

      eventToCrupdate.forEach(
          e -> {
            List<Event> duplicatedEvents = duplicateEventWithDifferentHour(e, eventSchedule);
            eventsToSave.addAll(duplicatedEvents);
          });
    } else {
      eventsToSave.addAll(eventToCrupdate);
    }
    return eventsToSave;
  }

  private List<Event> duplicateEventWithDifferentHour(
      Event event, Map<Instant, Instant> eventScheduled) {
    List<Event> duplicated = new ArrayList<>();
    duplicated.add(event);
    eventScheduled.forEach(
        (b, e) -> {
          Event newEvent =
              Event.builder()
                  .title(event.getTitle())
                  .colorCode(event.getColorCode())
                  .beginDatetime(b)
                  .endDatetime(e)
                  .type(event.getType())
                  .course(event.getCourse())
                  .description(event.getDescription())
                  .groups(event.getGroups())
                  .id(event.getId())
                  .planner(event.getPlanner())
                  .build();
          duplicated.add(newEvent);
        });
    return duplicated;
  }

  private Map<Instant, Instant> combineBeginningAndEndingHour(
      List<Instant> beginningHour, List<Instant> endingHour) {
    Map<Instant, Instant> eventScheduleMap = new HashMap<>();
    for (int i = 0; i < beginningHour.size(); i++) {
      eventScheduleMap.put(beginningHour.get(i), endingHour.get(i));
    }
    return eventScheduleMap;
  }

  private Optional<CreateEventFrequency> generateFrequency(CreateEventFrequency frequency) {
    if (!Objects.isNull(frequency.getFrequencyBeginningHour())
        || !Objects.isNull(frequency.getFrequencyEndingHour())
        || !Objects.isNull(frequency.getFrequencyScopeDay())
        || !Objects.isNull(frequency.getEventFrequencyNumber())) {
      return Optional.of(frequency);
    }
    return Optional.empty();
  }

  public Event deleteEvent(String eventId) {
    Event deleted = findEventById(eventId);

    if (deleted.isDeleted()) {
      throw new NotFoundException("Event with id #" + eventId + " not found");
    }

    eventRepository.deleteById(eventId);
    return deleted;
  }
}
