package school.hei.haapi.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import school.hei.haapi.endpoint.rest.model.EventType;

@Entity
@Table(name = "\"event\"")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "update \"event\" set is_deleted = true where id = ?")
@Where(clause = "is_deleted = false")
public class Event {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private EventType type;

  private String title;

  private String description;

  private String colorCode;

  private boolean isDeleted;

  // TODO : Add promotion

  @Column(name = "begin_datetime")
  private Instant beginDatetime;

  @Column(name = "end_datetime")
  private Instant endDatetime;

  @ManyToOne
  @JoinColumn(name = "planner_id", referencedColumnName = "id")
  private User planner;

  @ManyToOne
  @JoinColumn(name = "course_id", referencedColumnName = "id")
  private Course course;

  @ManyToMany(fetch = EAGER)
  @JoinTable(
      name = "event_group_participate",
      joinColumns = @JoinColumn(name = "event_id"),
      inverseJoinColumns = @JoinColumn(name = "group_id"))
  private List<Group> groups;

  @Override
  public String toString() {
    return "Event {id="
        + id
        + ", type="
        + type
        + ", title="
        + title
        + ", description="
        + description
        + ", colorCode="
        + colorCode
        + ", isDeleted"
        + isDeleted
        + "}";
  }
}
