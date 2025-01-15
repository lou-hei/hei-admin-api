package school.hei.haapi.endpoint.rest.mapper;

import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.CreateEvent;
import school.hei.haapi.endpoint.rest.model.GroupIdentifier;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.Group;
import school.hei.haapi.service.CourseService;
import school.hei.haapi.service.EventParticipantService;
import school.hei.haapi.service.GroupService;
import school.hei.haapi.service.UserService;

@Component
@AllArgsConstructor
public class EventMapper {
  private final CourseService courseService;
  private final GroupService groupService;
  private final UserService userService;
  private final CourseMapper courseMapper;
  private final UserMapper userMapper;
  private final GroupMapper groupMapper;
  private final EventParticipantService eventParticipantService;

  public school.hei.haapi.model.Event toDomain(CreateEvent createEvent) {
    return Event.builder()
        .id(createEvent.getId())
        .beginDatetime(createEvent.getBeginDatetime())
        .endDatetime(createEvent.getEndDatetime())
        .description(createEvent.getDescription())
        .colorCode(createEvent.getColorCode())
        .course(
            Objects.isNull(createEvent.getCourseId())
                ? null
                : courseService.getById(createEvent.getCourseId()))
        .groups(
            groupService.getAllById(
                Objects.requireNonNull(createEvent.getGroups()).stream()
                    .map(GroupIdentifier::getId)
                    .toList()))
        .planner(userService.findById(createEvent.getPlannerId()))
        .type(createEvent.getEventType())
        .title(createEvent.getTitle())
        .build();
  }

  public school.hei.haapi.endpoint.rest.model.Event toRest(Event domain) {
    List<Group> groups = domain.getGroups();
    return new school.hei.haapi.endpoint.rest.model.Event()
        .id(domain.getId())
        .endDatetime(domain.getEndDatetime())
        .beginDatetime(domain.getBeginDatetime())
        .description(domain.getDescription())
        .colorCode(domain.getColorCode())
        .type(domain.getType())
        .course(Objects.isNull(domain.getCourse()) ? null : courseMapper.toRest(domain.getCourse()))
        .title(domain.getTitle())
        .planner(userMapper.toIdentifier(domain.getPlanner()))
        .count(eventParticipantService.getEventParticipantsStats(domain.getId()))
        .groups(
            Objects.isNull(groups)
                ? List.of()
                : groups.stream().map(groupMapper::toRestGroupIdentifier).toList());
  }
}
