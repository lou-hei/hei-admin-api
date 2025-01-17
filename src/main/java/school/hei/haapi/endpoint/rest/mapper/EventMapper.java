package school.hei.haapi.endpoint.rest.mapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.CreateEvent;
import school.hei.haapi.endpoint.rest.model.GroupIdentifier;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.Group;
import school.hei.haapi.model.exception.NotFoundException;
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
    List<GroupIdentifier> groupIdentifiers = Objects.requireNonNull(createEvent.getGroups());
    List<Group> groups =
        groupService.getAllById(groupIdentifiers.stream().map(GroupIdentifier::getId).toList());
    groups.stream().map(group -> mapGroupFromGroupIdentifiers(group, groupIdentifiers)).toList();
    List<Group> mappedGroup = groupService.saveDomainGroup(groups);

    return Event.builder()
        .id(createEvent.getId())
        .beginDatetime(createEvent.getBeginDatetime())
        .endDatetime(createEvent.getEndDatetime())
        .description(createEvent.getDescription())
        .colorCode(createEvent.getColor())
        .course(
            Objects.isNull(createEvent.getCourseId())
                ? null
                : courseService.getById(createEvent.getCourseId()))
        .groups(mappedGroup)
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
        .color(domain.getColorCode())
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

  public Group mapGroupFromGroupIdentifiers(Group group, List<GroupIdentifier> groupIdentifiers) {
    String groupId = group.getId();
    Optional<GroupIdentifier> optionalGi =
        groupIdentifiers.stream().filter(gi -> groupId.equals(gi.getId())).findFirst();
    GroupIdentifier gi =
        optionalGi.orElseThrow(
            () -> new NotFoundException("group with id " + groupId + " not found"));
    group.setAttributedColor(gi.getAttributedColor());
    return group;
  }
}
