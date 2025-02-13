package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.EventMapper;
import school.hei.haapi.endpoint.rest.mapper.EventParticipantMapper;
import school.hei.haapi.endpoint.rest.model.AttendanceStatus;
import school.hei.haapi.endpoint.rest.model.CreateEvent;
import school.hei.haapi.endpoint.rest.model.Event;
import school.hei.haapi.endpoint.rest.model.EventParticipant;
import school.hei.haapi.endpoint.rest.model.EventParticipantStats;
import school.hei.haapi.endpoint.rest.model.EventStats;
import school.hei.haapi.endpoint.rest.model.EventType;
import school.hei.haapi.endpoint.rest.model.FrequencyScopeDay;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.endpoint.rest.model.UpdateEventParticipant;
import school.hei.haapi.endpoint.rest.validator.CreateEventFrequencyValidator;
import school.hei.haapi.http.model.CreateEventFrequency;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.EventFrequencyNumber;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.EventParticipantService;
import school.hei.haapi.service.EventService;
import school.hei.haapi.service.UserService;

@AllArgsConstructor
@RestController
public class EventController {
  private final EventMapper mapper;
  private final EventParticipantMapper eventParticipantMapper;
  private final EventService eventService;
  private final EventParticipantService eventParticipantService;
  private final UserService userService;
  private final CreateEventFrequencyValidator eventFrequencyValidator;

  @PutMapping("/events")
  public List<Event> crupdateEvents(
      @RequestBody List<CreateEvent> eventsToSave,
      @RequestParam(name = "frequency_day", required = false) FrequencyScopeDay frequencyScopeDay,
      @RequestParam(name = "frequency_number", required = false)
          EventFrequencyNumber eventFrequencyNumber,
      @RequestParam(name = "frequency_beginning_hour", required = false)
          String frequencyBeginningHour,
      @RequestParam(name = "frequency_ending_hour", required = false) String frequencyEndingHour) {
    CreateEventFrequency evenFrequency =
        CreateEventFrequency.builder()
            .frequencyScopeDay(frequencyScopeDay)
            .eventFrequencyNumber(eventFrequencyNumber)
            .frequencyBeginningHour(frequencyBeginningHour)
            .frequencyEndingHour(frequencyEndingHour)
            .build();
    eventFrequencyValidator.accept(evenFrequency);
    return eventService
        .createOrUpdateEvent(eventsToSave.stream().map(mapper::toDomain).toList(), evenFrequency)
        .stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/events")
  public List<Event> getEvents(
      @RequestParam(name = "page") PageFromOne page,
      @RequestParam(name = "page_size") BoundedPageSize pageSize,
      @RequestParam(name = "event_type", required = false) EventType eventType,
      @RequestParam(name = "group", required = false) Group group,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    return eventService.getEvents(from, to, title, eventType, group, page, pageSize).stream()
        .map(mapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/events/{event_id}")
  public Event getEventById(@PathVariable(name = "event_id") String eventId) {
    return mapper.toRest(eventService.findEventById(eventId));
  }

  @GetMapping("/events/stats")
  public EventStats getEventStats(
      @RequestParam(name = "id", required = false) String eventId,
      @RequestParam(name = "from", required = false) Instant from,
      @RequestParam(name = "to", required = false) Instant to) {

    return eventService.getStats(eventId, from, to);
  }

  @GetMapping("/events/{event_id}/participants")
  public List<EventParticipant> getEventParticipants(
      @PathVariable(name = "event_id") String eventId,
      @RequestParam(name = "page", defaultValue = "1") PageFromOne page,
      @RequestParam(name = "page_size", defaultValue = "15") BoundedPageSize pageSize,
      @RequestParam(name = "group_ref", required = false) String groupRef,
      @RequestParam(name = "student_ref", required = false) String ref,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "status", required = false) AttendanceStatus attendanceStatus) {
    return eventParticipantService
        .getEventParticipants(eventId, page, pageSize, groupRef, name, ref, attendanceStatus)
        .stream()
        .map(eventParticipantMapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping("/events/participants/{participant_id}/stats")
  public EventParticipantStats getEventParticipantStats(
      @PathVariable(name = "participant_id") String participantId,
      @RequestParam(name = "from_event_begin", required = false) Instant from,
      @RequestParam(name = "to_event_begin", required = false) Instant to) {
    Optional<Instant> optionalFrom = Optional.ofNullable(from);
    Optional<Instant> optionalTo = Optional.ofNullable(to);

    return eventParticipantService.getEventParticipantStats(
        participantId, optionalFrom, optionalTo);
  }

  @PutMapping("/events/{event_id}/participants")
  public List<EventParticipant> crupdateEventParticipantsToAnEvent(
      @PathVariable(name = "event_id") String eventId,
      @RequestBody List<UpdateEventParticipant> eventParticipantsToUpdate) {
    return eventParticipantService
        .updateEventParticipants(
            eventParticipantsToUpdate.stream()
                .map(eventParticipantMapper::toDomain)
                .collect(toUnmodifiableList()))
        .stream()
        .map(eventParticipantMapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping(value = "/event/{event_id}/students/raw/xlsx", produces = "application/vnd.ms-excel")
  public byte[] generateEventStudentsParticipantInXlsx(
      @PathVariable(name = "event_id") String eventId) {
    return userService.generateStudentsInEventXlsx(eventId);
  }

  @DeleteMapping("/events/{id}")
  public Event deleteEventById(@PathVariable(name = "id") String id) {
    return mapper.toRest(eventService.deleteEvent(id));
  }
}
