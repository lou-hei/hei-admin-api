package school.hei.haapi.service;

import static org.springframework.data.domain.Sort.Direction.ASC;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import school.hei.haapi.endpoint.rest.model.CreateGroupFlow;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.Group;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.Promotion;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.GroupRepository;
import school.hei.haapi.repository.UserRepository;
import school.hei.haapi.repository.dao.GroupDao;

@Service
@AllArgsConstructor
public class GroupService {
  // todo: to review all class

  private final GroupRepository repository;
  private final UserRepository userRepository;
  private final GroupFlowService groupFlowService;
  private final GroupRepository groupRepository;
  private final GroupDao groupDao;

  public Group findById(String groupId) {
    return repository
        .findById(groupId)
        .orElseThrow(() -> new NotFoundException("Group with id." + groupId + " not found"));
  }

  public List<Group> getAllById(List<String> groupsId) {
    try {
      return repository.findAllById(groupsId);
    } catch (Exception e) {
      throw new NotFoundException(e.getMessage());
    }
  }

  public List<Group> getAll(
      String ref, String studentRef, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(ASC, "ref"));
    return groupDao.findByCriteria(ref, studentRef, pageable);
  }

  public Integer getGroupSize(String groupId) {
    List<User> students = userRepository.findAllRemainingStudentsByGroupId(groupId);
    return students.size();
  }

  @Transactional
  public List<Group> saveAll(List<school.hei.haapi.model.notEntity.CreateGroup> createGroups) {
    List<school.hei.haapi.model.Group> groups = new ArrayList<>();
    List<CreateGroupFlow> createGroupFlows = new ArrayList<>();

    for (school.hei.haapi.model.notEntity.CreateGroup createGroup : createGroups) {
      Group group = repository.save(createGroup.getGroup());
      groups.add(group);

      if (createGroup.getStudents() != null) {
        for (String studentId : createGroup.getStudents()) {
          createGroupFlows.add(
              new CreateGroupFlow()
                  .moveType(CreateGroupFlow.MoveTypeEnum.JOIN)
                  .groupId(group.getId())
                  .studentId(studentId));
        }
      }
    }
    groupFlowService.saveAll(createGroupFlows);
    return groups;
  }

  public List<Group> getByUserId(String studentId) {
    return groupRepository.findByStudentId(studentId);
  }

  public List<Group> saveDomainGroup(List<Group> domainsGroup) {
    return repository.saveAll(domainsGroup);
  }

  public void updateGroups(Promotion promotion, String groupId) {
    Group group = findById(groupId);
    group.setPromotion(promotion);
    repository.save(group);
  }
}
