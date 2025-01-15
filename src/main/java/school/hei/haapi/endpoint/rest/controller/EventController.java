package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.haapi.endpoint.rest.mapper.EventMapper;
import school.hei.haapi.endpoint.rest.mapper.EventParticipantMapper;
import school.hei.haapi.endpoint.rest.model.CreateEvent;
import school.hei.haapi.endpoint.rest.model.Event;
import school.hei.haapi.endpoint.rest.model.EventParticipant;
import school.hei.haapi.endpoint.rest.model.EventType;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.endpoint.rest.model.UpdateEventParticipant;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.EventParticipantService;
import school.hei.haapi.service.EventService;

@AllArgsConstructor
@RestController
public class EventController {
  private final EventMapper mapper;
  private final EventParticipantMapper eventParticipantMapper;
  private final EventService eventService;
  private final EventParticipantService eventParticipantService;

  @PutMapping("/events")
  public List<Event> crupdateEvents(@RequestBody List<CreateEvent> eventsToSave) {
    return eventService
        .createOrUpdateEvent(eventsToSave.stream().map(mapper::toDomain).toList())
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

  @GetMapping("/events/{event_id}/participants")
  public List<EventParticipant> getEventParticipants(
      @PathVariable(name = "event_id") String eventId,
      @RequestParam(name = "page", defaultValue = "1") PageFromOne page,
      @RequestParam(name = "page_size", defaultValue = "15") BoundedPageSize pageSize,
      @RequestParam(name = "group_ref", required = false) String groupRef) {
    return eventParticipantService.getEventParticipants(eventId, page, pageSize, groupRef).stream()
        .map(eventParticipantMapper::toRest)
        .collect(toUnmodifiableList());
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
}
