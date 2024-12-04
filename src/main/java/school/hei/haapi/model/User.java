package school.hei.haapi.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import school.hei.haapi.endpoint.rest.model.SpecializationField;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
// TODO: separate to a child table as MANAGER, TEACHER, STUDENT, MONITOR
public class User implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String firstName;

  @NotBlank(message = "Last name is mandatory")
  private String lastName;

  @NotBlank(message = "Email is mandatory")
  @Email(message = "Email must be valid")
  private String email;

  @NotBlank(message = "Reference is mandatory")
  private String ref;

  private String nic;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Status status;

  private String phone;

  private String degree;

  private String function;

  private LocalDate birthDate;

  private String birthPlace;

  private Instant entranceDatetime;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private SpecializationField specializationField;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Sex sex;

  private String address;

  @Column(name = "\"role\"")
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Role role;

  private String profilePictureKey;

  // RELATION (TEACHER): Awarded Courses
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "mainTeacher")
  @ToString.Exclude
  private List<AwardedCourse> awardedCourses;

  // RELATION (STUDENT): Group Flows
  @OneToMany(mappedBy = "student", fetch = LAZY)
  @ToString.Exclude
  private List<GroupFlow> groupFlows;

  // RELATION (STUDENT): Grades
  @OneToMany(mappedBy = "student", fetch = LAZY)
  @ToString.Exclude
  private List<Grade> grades;

  // RELATION (STUDENT): Work Documents
  @OneToMany(mappedBy = "student", fetch = LAZY)
  private List<WorkDocument> workDocuments;

  // RELATION (MONITOR - STUDENT): Which Monitor follows which students or which student is
  // following by which monitor
  @ManyToMany(fetch = LAZY)
  @JoinTable(
      name = "\"monitor_following_student\"",
      joinColumns = @JoinColumn(name = "\"monitor_id\""),
      inverseJoinColumns = @JoinColumn(name = "\"student_id\""))
  private List<User> monitors;

  private Double longitude;
  private Double latitude;

  private String highSchoolOrigin;

  @OneToMany(mappedBy = "user", fetch = LAZY)
  private List<Letter> letters;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    User user = (User) o;
    return id != null && Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "User{"
        + "id='"
        + id
        + '\''
        + ", firstName='"
        + firstName
        + '\''
        + ", lastName='"
        + lastName
        + '\''
        + ", email='"
        + email
        + '\''
        + ", ref='"
        + ref
        + '\''
        + ", nic='"
        + nic
        + '\''
        + ", status="
        + status
        + ", phone='"
        + phone
        + '\''
        + ", birthDate="
        + birthDate
        + ", birthPlace='"
        + birthPlace
        + '\''
        + ", entranceDatetime="
        + entranceDatetime
        + ", specializationField="
        + specializationField
        + ", sex="
        + sex
        + ", address='"
        + address
        + '\''
        + ", role="
        + role
        + ", profilePictureKey='"
        + profilePictureKey
        + '\''
        + ", highSchoolOrigin='"
        + highSchoolOrigin
        + '}';
  }

  public enum Sex {
    M,
    F;
  }

  public enum Status {
    ENABLED,
    DISABLED,
    SUSPENDED;
  }

  public enum Role {
    STAFF_MEMBER,
    ADMIN,
    MONITOR,
    STUDENT,
    TEACHER,
    MANAGER;
  }
}
