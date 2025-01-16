package school.hei.haapi.service;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static school.hei.haapi.endpoint.rest.model.WorkStudyStatus.*;
import static school.hei.haapi.model.User.Role.STUDENT;
import static school.hei.haapi.model.User.Role.TEACHER;
import static school.hei.haapi.model.User.Sex.F;
import static school.hei.haapi.model.User.Sex.M;
import static school.hei.haapi.model.User.Status.DISABLED;
import static school.hei.haapi.model.User.Status.ENABLED;
import static school.hei.haapi.model.User.Status.SUSPENDED;
import static school.hei.haapi.service.aws.FileService.getFormattedBucketKey;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.UserUpserted;
import school.hei.haapi.endpoint.rest.model.*;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.model.validator.UserValidator;
import school.hei.haapi.repository.GroupRepository;
import school.hei.haapi.repository.UserRepository;
import school.hei.haapi.repository.dao.StudentDao;
import school.hei.haapi.repository.dao.UserManagerDao;
import school.hei.haapi.service.aws.FileService;
import school.hei.haapi.service.utils.XlsxCellsGenerator;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final EventProducer eventProducer;
  private final UserValidator userValidator;
  private final UserManagerDao userManagerDao;
  private final FileService fileService;
  private final MultipartFileConverter fileConverter;
  private final GroupRepository groupRepository;
  private final MonitoringStudentService monitoringStudentService;
  private final FeeService feeService;
  private final StudentDao studentDao;

  public void uploadUserProfilePicture(MultipartFile profilePictureAsMultipartFile, String userId) {
    User user = findById(userId);
    File savedProfilePicture = fileConverter.apply(profilePictureAsMultipartFile);
    String bucketKey =
        getFormattedBucketKey(user, "PROFILE_PICTURE")
            + fileService.getFileExtension(profilePictureAsMultipartFile);
    user.setProfilePictureKey(bucketKey);
    userRepository.save(user);
    fileService.uploadObjectToS3Bucket(bucketKey, savedProfilePicture);
  }

  @Transactional
  public void suspendStudentById(String suspendedStudentId) {
    userManagerDao.updateUserStatusById(SUSPENDED, suspendedStudentId);
  }

  public User updateUser(User user, String userId) {
    User toUpdate = refreshUserById(userId, user);
    return userRepository.save(toUpdate);
  }

  private User refreshUserById(String userId, User refreshedUser) {
    User userToRefresh = findById(userId);

    userToRefresh.setAddress(refreshedUser.getAddress());
    userToRefresh.setBirthDate(refreshedUser.getBirthDate());
    userToRefresh.setFirstName(refreshedUser.getFirstName());
    userToRefresh.setLastName(refreshedUser.getLastName());
    userToRefresh.setSex(refreshedUser.getSex());
    userToRefresh.setPhone(refreshedUser.getPhone());
    userToRefresh.setNic(refreshedUser.getNic());
    userToRefresh.setBirthPlace(refreshedUser.getBirthPlace());
    userToRefresh.setLongitude(refreshedUser.getLongitude());
    userToRefresh.setLatitude(refreshedUser.getLatitude());
    userToRefresh.setEntranceDatetime(refreshedUser.getEntranceDatetime());
    userToRefresh.setStatus(refreshedUser.getStatus());
    userToRefresh.setSpecializationField(refreshedUser.getSpecializationField());
    userToRefresh.setHighSchoolOrigin(refreshedUser.getHighSchoolOrigin());
    userToRefresh.setDegree(refreshedUser.getDegree());
    userToRefresh.setCnaps(refreshedUser.getCnaps());
    userToRefresh.setEndingService(refreshedUser.getEndingService());
    userToRefresh.setOstie(refreshedUser.getOstie());
    userToRefresh.setFunction(refreshedUser.getFunction());
    return userToRefresh;
  }

  public User findById(String userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new NotFoundException("User with id: " + userId + " not found"));
  }

  public User getByEmail(String email) {
    return userRepository.getByEmail(email);
  }

  @Transactional
  public List<User> saveAll(List<User> users) {
    userValidator.accept(users);
    // TODO: do not nullify profile picture here
    List<User> savedUsers = userRepository.saveAll(users);
    eventProducer.accept(
        users.stream().map(this::toUserUpsertedEvent).collect(toUnmodifiableList()));
    return savedUsers;
  }

  @Transactional
  public List<User> saveAll(
      HashMap<User, PaymentFrequency> userPaymentFrequencyMap, Instant firstDueDatetime) {
    List<User> users = new ArrayList<>(userPaymentFrequencyMap.keySet());
    userValidator.accept(users);
    List<User> savedUsers = userRepository.saveAll(users);
    eventProducer.accept(
        users.stream().map(this::toUserUpsertedEvent).collect(toUnmodifiableList()));

    // TODO: handle existing users exception when creating fees automatically
    for (Map.Entry<User, PaymentFrequency> entry : userPaymentFrequencyMap.entrySet()) {
      if (entry.getValue() != null)
        feeService.saveFromPaymentFrequency(entry.getKey(), entry.getValue(), firstDueDatetime);
    }

    return savedUsers;
  }

  private UserUpserted toUserUpsertedEvent(User user) {
    return new UserUpserted().userId(user.getId()).email(user.getEmail());
  }

  public List<User> getByRole(
      User.Role role,
      PageFromOne page,
      BoundedPageSize pageSize,
      User.Status status,
      User.Sex sex) {
    return getByCriteria(role, "", "", "", page, pageSize, status, sex);
  }

  public List<User> getAll() {
    return userRepository.findAll();
  }

  public List<User> getByRoleAndStatus(User.Role role, User.Status status) {
    return userRepository.findAllByRoleAndStatus(role, status);
  }

  public <T> byte[] getByRoleAndStatusAsXlsx(
      User.Role role, User.Status status, Function<User, T> mapper) {
    List<User> users = getByRoleAndStatus(role, status);
    XlsxCellsGenerator<T> generator = new XlsxCellsGenerator<>();
    List<T> mappedUsers = users.stream().map(mapper).toList();

    return generator.apply(
        mappedUsers,
        List.of(
            "ref",
            "firstName",
            "lastName",
            "sex",
            "phone",
            "email",
            "nic",
            "function",
            "ostie",
            "cnaps",
            "address"));
  }

  public List<User> getAllEnabledUsers() {
    return userRepository.findAllByStatus(ENABLED);
  }

  public List<User> getAllSuspendedUsers() {
    return userRepository.findAllByStatus(SUSPENDED);
  }

  public List<User> getByCriteria(
      User.Role role,
      String firstName,
      String lastName,
      String ref,
      PageFromOne page,
      BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(ASC, "ref"));
    return userManagerDao.findByCriteria(
        role, ref, firstName, lastName, pageable, null, null, null, null, null, null, null);
  }

  public List<User> getByCriteria(
      User.Role role,
      String firstName,
      String lastName,
      String ref,
      PageFromOne page,
      BoundedPageSize pageSize,
      User.Status status,
      User.Sex sex) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(ASC, "ref"));

    return userManagerDao.findByCriteria(
        role, ref, firstName, lastName, pageable, status, sex, null, null, null, null, null);
  }

  public List<User> getByLinkedCourse(
      User.Role role,
      String firstName,
      String lastName,
      String ref,
      String courseId,
      PageFromOne page,
      BoundedPageSize pageSize,
      User.Status status,
      User.Sex sex,
      WorkStudyStatus workStatus,
      Instant commitmentBeginDate,
      List<String> excludeGroupIds) {
    log.info("Page = {}", page.getValue());
    log.info("PageSize = {}", pageSize.getValue());
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(ASC, "ref"));
    return userManagerDao.findByCriteria(
        role,
        ref,
        firstName,
        lastName,
        pageable,
        status,
        sex,
        workStatus,
        commitmentBeginDate,
        courseId,
        Instant.now(),
        excludeGroupIds);
  }

  public List<User> getByGroupId(String groupId) {
    return userRepository.findAllRemainingStudentsByGroupId(groupId);
  }

  public byte[] generateStudentsGroup(String groupId) {
    XlsxCellsGenerator<User> xlsxCellsGenerator = new XlsxCellsGenerator<>();
    List<User> studentsGroup = getByGroupId(groupId);
    return xlsxCellsGenerator.apply(studentsGroup, List.of("ref", "firstName", "lastName"));
  }

  public byte[] generateTeachersXlsx() {
    XlsxCellsGenerator<User> xlsxCellsGenerator = new XlsxCellsGenerator<>();
    List<User> teachers = userRepository.findAllByRoleAndStatus(TEACHER, ENABLED);
    return xlsxCellsGenerator.apply(teachers, List.of("firstName", "lastName", "email", "sex"));
  }

  public List<User> getByGroupIdWithFilter(
      String groupId, PageFromOne page, BoundedPageSize pageSize, String studentFirstname) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(ASC, "ref"));
    return userRepository
        .findStudentGroupsWithFilter(groupId, studentFirstname, pageable)
        .getContent();
  }

  public List<User> getAllStudentNotDisabled() {
    return userRepository.findAllStudentNotDisabled();
  }

  public Statistics getStudentsStat(List<Student> students) {
    int willBeWorkingNb = getStudentsAlternatingSize(students, WILL_BE_WORKING);
    int haveBeenWorkingNb = getStudentsAlternatingSize(students, HAVE_BEEN_WORKING);
    int workingNb = getStudentsAlternatingSize(students, WORKING);
    int notWorkingNb = getStudentsAlternatingSize(students, NOT_WORKING);
    return new Statistics()
        .women(
            new StatisticsDetails()
                .disabled(userRepository.countBySexAndRoleAndStatus(F, STUDENT, DISABLED))
                .suspended(userRepository.countBySexAndRoleAndStatus(F, STUDENT, SUSPENDED))
                .enabled(userRepository.countBySexAndRoleAndStatus(F, STUDENT, ENABLED))
                .total(userRepository.countBySexAndRole(F, STUDENT)))
        .totalGroups((int) groupRepository.count())
        .totalStudents(userRepository.countByRole(STUDENT))
        .men(
            new StatisticsDetails()
                .disabled(userRepository.countBySexAndRoleAndStatus(M, STUDENT, DISABLED))
                .suspended(userRepository.countBySexAndRoleAndStatus(M, STUDENT, SUSPENDED))
                .enabled(userRepository.countBySexAndRoleAndStatus(M, STUDENT, ENABLED))
                .total(userRepository.countBySexAndRole(M, STUDENT)))
        .studentsAlternating(
            new StatisticsStudentsAlternating()
                .total(willBeWorkingNb + haveBeenWorkingNb + workingNb)
                .haveBeenWorking(haveBeenWorkingNb)
                .working(workingNb)
                .notWorking(notWorkingNb)
                .willWork(willBeWorkingNb));
  }

  public int getStudentsAlternatingSize(List<Student> students, WorkStudyStatus workStudyStatus) {
    // TODO: use long
    return (int)
        students.stream()
            .filter(student -> Objects.equals(student.getWorkStudyStatus(), workStudyStatus))
            .count();
  }

  public List<User> findMonitorsByStudentId(String studentId) {
    return monitoringStudentService.getMonitorsByStudentId(studentId);
  }

  public List<User> getStudentsByPromotionId(String promotionId) {
    return userRepository.findAllStudentsByPromotionId(promotionId);
  }

  public List<User> getStudentsWithUnpaidOrLateFee() {
    return userRepository.getStudentsWithUnpaidOrLateFee();
  }

  public byte[] generateStudentsInXlsx(String eventId) {
    XlsxCellsGenerator<User> xlsxCellsGenerator = new XlsxCellsGenerator<>();
    List<User> students = studentDao.getStudentsByCriteria(eventId);
    return xlsxCellsGenerator.apply(students, List.of("firstName", "lastName", "email", "sex"));
  }
}
