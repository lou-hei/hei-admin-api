package school.hei.haapi.endpoint.rest.mapper;

import static school.hei.haapi.endpoint.rest.mapper.FileInfoMapper.ONE_DAY_DURATION_AS_LONG;
import static school.hei.haapi.model.User.Role.GATE_KEEPER;
import static school.hei.haapi.model.User.Role.ORGANIZER;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.*;
import school.hei.haapi.model.User;
import school.hei.haapi.model.WorkDocument;
import school.hei.haapi.service.GroupService;
import school.hei.haapi.service.UserService;
import school.hei.haapi.service.WorkDocumentService;
import school.hei.haapi.service.aws.FileService;
import school.hei.haapi.service.utils.IsStudentRepeatingYear;

@Component
@AllArgsConstructor
@Slf4j
public class UserMapper {
  private final WorkDocumentService workDocumentService;
  private final StatusEnumMapper statusEnumMapper;
  private final SexEnumMapper sexEnumMapper;
  private final FileService fileService;
  private final GroupService groupService;
  private final GroupMapper groupMapper;
  private final IsStudentRepeatingYear isStudentRepeatingYear;
  private final UserService userService;

  public UserIdentifier toIdentifier(User user) {
    return new UserIdentifier()
        .id(user.getId())
        .ref(user.getRef())
        .nic(user.getNic())
        .lastName(user.getLastName())
        .firstName(user.getFirstName())
        .email(user.getEmail());
  }

  public StaffMember toRestStaffMember(User user) {
    StaffMember staffMember = new StaffMember();
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    staffMember.setId(user.getId());
    staffMember.setFirstName(user.getFirstName());
    staffMember.setLastName(user.getLastName());
    staffMember.setEmail(user.getEmail());
    staffMember.setRef(user.getRef());
    staffMember.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    staffMember.setPhone(user.getPhone());
    staffMember.setEntranceDatetime(user.getEntranceDatetime());
    staffMember.setBirthDate(user.getBirthDate());
    staffMember.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    staffMember.setAddress(user.getAddress());
    staffMember.setBirthPlace(user.getBirthPlace());
    staffMember.setNic(user.getNic());
    staffMember.setProfilePicture(url);
    staffMember.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    staffMember.setHighSchoolOrigin(user.getHighSchoolOrigin());
    staffMember.setCnaps(user.getCnaps());
    staffMember.setDegree(user.getDegree());
    staffMember.setFunction(user.getFunction());
    staffMember.setEndingService(user.getEndingService());
    staffMember.setOstie(user.getOstie());

    log.info(staffMember.toString());
    log.info("to rest aiza");
    return staffMember;
  }

  public Student toRestStudent(User user) {
    Student restStudent = new Student();
    Optional<WorkDocument> studentLastWorkDocument =
        workDocumentService.findLastWorkDocumentByStudentId(user.getId());
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    restStudent.setId(user.getId());
    restStudent.setFirstName(user.getFirstName());
    restStudent.setLastName(user.getLastName());
    restStudent.setEmail(user.getEmail());
    restStudent.setRef(user.getRef());
    restStudent.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    restStudent.setPhone(user.getPhone());
    restStudent.setEntranceDatetime(user.getEntranceDatetime());
    restStudent.setBirthDate(user.getBirthDate());
    restStudent.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    restStudent.setAddress(user.getAddress());
    restStudent.setNic(user.getNic());
    restStudent.setBirthPlace(user.getBirthPlace());
    restStudent.setSpecializationField(user.getSpecializationField());
    restStudent.setProfilePicture(url);
    restStudent.groups(
        groupService.getByUserId(user.getId()).stream().map(groupMapper::toRest).toList());
    restStudent.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    restStudent.setHighSchoolOrigin(user.getHighSchoolOrigin());
    restStudent.setWorkStudyStatus(
        workDocumentService.defineStudentWorkStatusFromWorkDocumentDetails(
            studentLastWorkDocument));
    restStudent.setProfessionalExperience(
        workDocumentService.defineStudentProfessionalExperienceStatus(studentLastWorkDocument));
    restStudent.setCommitmentBeginDate(
        workDocumentService.defineStudentCommitmentBegin(studentLastWorkDocument));
    restStudent.setCommitmentEndDate(
        workDocumentService.defineStudentCommitmentEnd(studentLastWorkDocument));
    restStudent.setIsRepeatingYear(isStudentRepeatingYear.apply(user));
    return restStudent;
  }

  public Teacher toRestTeacher(User user) {
    Teacher teacher = new Teacher();
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    teacher.setId(user.getId());
    teacher.setFirstName(user.getFirstName());
    teacher.setLastName(user.getLastName());
    teacher.setEmail(user.getEmail());
    teacher.setRef(user.getRef());
    teacher.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    teacher.setPhone(user.getPhone());
    teacher.setEntranceDatetime(user.getEntranceDatetime());
    teacher.setBirthDate(user.getBirthDate());
    teacher.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    teacher.setAddress(user.getAddress());
    teacher.setBirthPlace(user.getBirthPlace());
    teacher.setNic(user.getNic());
    teacher.setProfilePicture(url);
    teacher.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    teacher.setHighSchoolOrigin(user.getHighSchoolOrigin());
    return teacher;
  }

  public Manager toRestManager(User user) {
    Manager manager = new Manager();
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    manager.setId(user.getId());
    manager.setFirstName(user.getFirstName());
    manager.setLastName(user.getLastName());
    manager.setEmail(user.getEmail());
    manager.setRef(user.getRef());
    manager.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    manager.setPhone(user.getPhone());
    manager.setEntranceDatetime(user.getEntranceDatetime());
    manager.setBirthDate(user.getBirthDate());
    manager.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    manager.setAddress(user.getAddress());
    manager.setBirthPlace(user.getBirthPlace());
    manager.setNic(user.getNic());
    manager.setProfilePicture(url);
    manager.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    manager.setHighSchoolOrigin(user.getHighSchoolOrigin());
    return manager;
  }

  public Admin toRestAdmin(User user) {
    Admin admin = new Admin();
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    admin.setId(user.getId());
    admin.setFirstName(user.getFirstName());
    admin.setLastName(user.getLastName());
    admin.setEmail(user.getEmail());
    admin.setRef(user.getRef());
    admin.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    admin.setPhone(user.getPhone());
    admin.setEntranceDatetime(user.getEntranceDatetime());
    admin.setBirthDate(user.getBirthDate());
    admin.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    admin.setAddress(user.getAddress());
    admin.setBirthPlace(user.getBirthPlace());
    admin.setNic(user.getNic());
    admin.setProfilePicture(url);
    admin.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    admin.setHighSchoolOrigin(user.getHighSchoolOrigin());
    return admin;
  }

  public Monitor toRestMonitor(User user) {
    Monitor monitor = new Monitor();
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    monitor.setId(user.getId());
    monitor.setFirstName(user.getFirstName());
    monitor.setLastName(user.getLastName());
    monitor.setEmail(user.getEmail());
    monitor.setRef(user.getRef());
    monitor.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    monitor.setPhone(user.getPhone());
    monitor.setEntranceDatetime(user.getEntranceDatetime());
    monitor.setBirthDate(user.getBirthDate());
    monitor.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    monitor.setAddress(user.getAddress());
    monitor.setBirthPlace(user.getBirthPlace());
    monitor.setNic(user.getNic());
    monitor.setProfilePicture(url);
    monitor.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    monitor.setHighSchoolOrigin(user.getHighSchoolOrigin());
    return monitor;
  }

  public Organizer toRestOrganizer(User user) {
    Organizer organizer = new Organizer();
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    organizer.setId(user.getId());
    organizer.setFirstName(user.getFirstName());
    organizer.setLastName(user.getLastName());
    organizer.setEmail(user.getEmail());
    organizer.setRef(user.getRef());
    organizer.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    organizer.setPhone(user.getPhone());
    organizer.setEntranceDatetime(user.getEntranceDatetime());
    organizer.setBirthDate(user.getBirthDate());
    organizer.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    organizer.setAddress(user.getAddress());
    organizer.setBirthPlace(user.getBirthPlace());
    organizer.setNic(user.getNic());
    organizer.setProfilePicture(url);
    organizer.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    organizer.setHighSchoolOrigin(user.getHighSchoolOrigin());
    return organizer;
  }

  public GateKeeper toRestGateKeeper(User user) {
    GateKeeper gateKeeper = new GateKeeper();
    String profilePictureKey = user.getProfilePictureKey();
    String url =
        profilePictureKey != null
            ? fileService.getPresignedUrl(profilePictureKey, ONE_DAY_DURATION_AS_LONG)
            : null;

    gateKeeper.setId(user.getId());
    gateKeeper.setFirstName(user.getFirstName());
    gateKeeper.setLastName(user.getLastName());
    gateKeeper.setEmail(user.getEmail());
    gateKeeper.setRef(user.getRef());
    gateKeeper.setStatus(statusEnumMapper.toRestStatus(user.getStatus()));
    gateKeeper.setPhone(user.getPhone());
    gateKeeper.setEntranceDatetime(user.getEntranceDatetime());
    gateKeeper.setBirthDate(user.getBirthDate());
    gateKeeper.setSex(sexEnumMapper.toRestSexEnum(user.getSex()));
    gateKeeper.setAddress(user.getAddress());
    gateKeeper.setBirthPlace(user.getBirthPlace());
    gateKeeper.setNic(user.getNic());
    gateKeeper.setProfilePicture(url);
    gateKeeper.setCoordinates(
        new Coordinates().longitude(user.getLongitude()).latitude(user.getLatitude()));
    gateKeeper.setHighSchoolOrigin(user.getHighSchoolOrigin());
    return gateKeeper;
  }

  public User toDomain(CrupdateManager manager) {
    return User.builder()
        .role(User.Role.MANAGER)
        .id(manager.getId())
        .firstName(manager.getFirstName())
        .lastName(manager.getLastName())
        .email(manager.getEmail())
        .ref(manager.getRef())
        .status(statusEnumMapper.toDomainStatus(manager.getStatus()))
        .phone(manager.getPhone())
        .entranceDatetime(manager.getEntranceDatetime())
        .birthDate(manager.getBirthDate())
        .sex(sexEnumMapper.toDomainSexEnum(manager.getSex()))
        .address(manager.getAddress())
        .nic(manager.getNic())
        .birthPlace(manager.getBirthPlace())
        .longitude(manager.getCoordinates().getLongitude())
        .latitude(manager.getCoordinates().getLatitude())
        .highSchoolOrigin(manager.getHighSchoolOrigin())
        .build();
  }

  public User toDomain(CrupdateTeacher teacher) {
    return User.builder()
        .role(User.Role.TEACHER)
        .id(teacher.getId())
        .firstName(teacher.getFirstName())
        .lastName(teacher.getLastName())
        .email(teacher.getEmail())
        .ref(teacher.getRef())
        .status(statusEnumMapper.toDomainStatus(teacher.getStatus()))
        .phone(teacher.getPhone())
        .entranceDatetime(teacher.getEntranceDatetime())
        .birthDate(teacher.getBirthDate())
        .sex(sexEnumMapper.toDomainSexEnum(teacher.getSex()))
        .address(teacher.getAddress())
        .nic(teacher.getNic())
        .birthPlace(teacher.getBirthPlace())
        .longitude(teacher.getCoordinates().getLongitude())
        .latitude(teacher.getCoordinates().getLatitude())
        .highSchoolOrigin(teacher.getHighSchoolOrigin())
        .build();
  }

  public User toDomain(StaffMember teacher) {
    return User.builder()
        .role(User.Role.STAFF_MEMBER)
        .id(teacher.getId())
        .firstName(teacher.getFirstName())
        .lastName(teacher.getLastName())
        .email(teacher.getEmail())
        .ref(teacher.getRef())
        .status(statusEnumMapper.toDomainStatus(teacher.getStatus()))
        .phone(teacher.getPhone())
        .entranceDatetime(teacher.getEntranceDatetime())
        .birthDate(teacher.getBirthDate())
        .sex(sexEnumMapper.toDomainSexEnum(teacher.getSex()))
        .address(teacher.getAddress())
        .nic(teacher.getNic())
        .birthPlace(teacher.getBirthPlace())
        .longitude(teacher.getCoordinates().getLongitude())
        .latitude(teacher.getCoordinates().getLatitude())
        .highSchoolOrigin(teacher.getHighSchoolOrigin())
        .degree(teacher.getDegree())
        .function(teacher.getFunction())
        .ostie(teacher.getOstie())
        .cnaps(teacher.getCnaps())
        .endingService(teacher.getEndingService())
        .build();
  }

  public User toDomain(CrupdateStudent student) {
    return User.builder()
        .role(User.Role.STUDENT)
        .id(student.getId())
        .firstName(student.getFirstName())
        .lastName(student.getLastName())
        .email(student.getEmail())
        .ref(student.getRef())
        .status(statusEnumMapper.toDomainStatus(student.getStatus()))
        .phone(student.getPhone())
        .entranceDatetime(student.getEntranceDatetime())
        .birthDate(student.getBirthDate())
        .sex(sexEnumMapper.toDomainSexEnum(student.getSex()))
        .address(student.getAddress())
        .birthPlace(student.getBirthPlace())
        .nic(student.getNic())
        .specializationField(student.getSpecializationField())
        .longitude(student.getCoordinates().getLongitude())
        .latitude(student.getCoordinates().getLatitude())
        .highSchoolOrigin(student.getHighSchoolOrigin())
        .build();
  }

  public HashMap<User, PaymentFrequency> toMapDomain(List<CrupdateStudent> students) {
    HashMap<User, PaymentFrequency> map = new HashMap<>();
    for (CrupdateStudent student : students) {
      map.put(toDomain(student), student.getPaymentFrequency());
    }
    return map;
  }

  public User toDomain(CrupdateMonitor monitor) {
    return User.builder()
        .role(User.Role.MONITOR)
        .id(monitor.getId())
        .firstName(monitor.getFirstName())
        .lastName(monitor.getLastName())
        .email(monitor.getEmail())
        .ref(monitor.getRef())
        .status(statusEnumMapper.toDomainStatus(monitor.getStatus()))
        .phone(monitor.getPhone())
        .entranceDatetime(monitor.getEntranceDatetime())
        .birthDate(monitor.getBirthDate())
        .sex(sexEnumMapper.toDomainSexEnum(monitor.getSex()))
        .address(monitor.getAddress())
        .nic(monitor.getNic())
        .birthPlace(monitor.getBirthPlace())
        .longitude(monitor.getCoordinates().getLongitude())
        .latitude(monitor.getCoordinates().getLatitude())
        .highSchoolOrigin(monitor.getHighSchoolOrigin())
        .build();
  }

  public User toDomain(CrupdateGateKeeper gateKeeper) {
    return User.builder()
        .role(GATE_KEEPER)
        .id(gateKeeper.getId())
        .firstName(gateKeeper.getFirstName())
        .lastName(gateKeeper.getLastName())
        .email(gateKeeper.getEmail())
        .ref(gateKeeper.getRef())
        .status(statusEnumMapper.toDomainStatus(gateKeeper.getStatus()))
        .phone(gateKeeper.getPhone())
        .entranceDatetime(gateKeeper.getEntranceDatetime())
        .birthDate(gateKeeper.getBirthDate())
        .sex(sexEnumMapper.toDomainSexEnum(gateKeeper.getSex()))
        .address(gateKeeper.getAddress())
        .nic(gateKeeper.getNic())
        .birthPlace(gateKeeper.getBirthPlace())
        .longitude(gateKeeper.getCoordinates().getLongitude())
        .latitude(gateKeeper.getCoordinates().getLatitude())
        .highSchoolOrigin(gateKeeper.getHighSchoolOrigin())
        .build();
  }

  public User toDomain(CrupdateOrganizer organizer) {
    return User.builder()
        .role(ORGANIZER)
        .id(organizer.getId())
        .firstName(organizer.getFirstName())
        .lastName(organizer.getLastName())
        .email(organizer.getEmail())
        .ref(organizer.getRef())
        .status(statusEnumMapper.toDomainStatus(organizer.getStatus()))
        .phone(organizer.getPhone())
        .entranceDatetime(organizer.getEntranceDatetime())
        .birthDate(organizer.getBirthDate())
        .sex(sexEnumMapper.toDomainSexEnum(organizer.getSex()))
        .address(organizer.getAddress())
        .nic(organizer.getNic())
        .birthPlace(organizer.getBirthPlace())
        .longitude(organizer.getCoordinates().getLongitude())
        .latitude(organizer.getCoordinates().getLatitude())
        .highSchoolOrigin(organizer.getHighSchoolOrigin())
        .build();
  }

  public User toDomain(UserIdentifier userIdentifier) {
    return userService.findById(userIdentifier.getId());
  }
}
