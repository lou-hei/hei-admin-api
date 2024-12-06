package school.hei.haapi.model;

import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import school.hei.haapi.endpoint.rest.model.FeeStatusEnum;
import school.hei.haapi.endpoint.rest.model.FeeTypeEnum;
import school.hei.haapi.model.Mpbs.Mpbs;

@Entity
@Table(name = "\"fee\"")
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "update \"fee\" set is_deleted = true where id = ?")
@Where(clause = "is_deleted = false")
public class Fee implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User student;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private FeeStatusEnum status;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private FeeTypeEnum type;

  private Integer totalAmount;

  private Instant updatedAt;

  private Integer remainingAmount;

  private String comment;

  private boolean isDeleted;

  @CreationTimestamp
  @Getter(AccessLevel.NONE)
  private Instant creationDatetime;

  private Instant dueDatetime;

  @OneToMany(mappedBy = "fee", cascade = REMOVE)
  private List<Payment> payments;

  @OneToOne(mappedBy = "fee")
  private Mpbs mpbs;

  public Instant getCreationDatetime() {
    return creationDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

  public Fee (Fee fee) {
    this.id = fee.getId();
    this.student = fee.getStudent();
    this.status = fee.getStatus();
    this.type = fee.getType();
    this.totalAmount = fee.getTotalAmount();
    this.remainingAmount = fee.getRemainingAmount();
    this.comment = fee.getComment();
    this.mpbs = fee.getMpbs();
    this.creationDatetime = fee.getCreationDatetime();
    this.dueDatetime = fee.getDueDatetime();
    this.payments = fee.getPayments();
    this.isDeleted = fee.isDeleted();
    this.updatedAt = fee.getUpdatedAt();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    Fee fee = (Fee) o;
    return totalAmount == fee.totalAmount
        && remainingAmount == fee.remainingAmount
        && Objects.equals(id, fee.id)
        && Objects.equals(student.getId(), fee.student.getId())
        && status == fee.status
        && type == fee.type
        && Objects.equals(creationDatetime, fee.creationDatetime)
        && Objects.equals(dueDatetime, fee.dueDatetime);
  }

  public String describe() {
    return """
Fee : {"id" : "%s", "remainingAmount" : "%s", "totalAmount" : "%s", "dueDatetime" : "%s", "actualStatus" : "%s"}
"""
        .formatted(getId(), getRemainingAmount(), getTotalAmount(), getDueDatetime(), getStatus());
  }

  @Override
  public String toString() {
    return "Fee{"
        + "id='"
        + id
        + '\''
        + ", status="
        + status
        + ", type="
        + type
        + ", totalAmount="
        + totalAmount
        + ", updatedAt="
        + updatedAt
        + ", remainingAmount="
        + remainingAmount
        + ", comment='"
        + comment
        + '\''
        + ", isDeleted="
        + isDeleted
        + ", creationDatetime="
        + creationDatetime
        + ", dueDatetime="
        + dueDatetime
        + '}';
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
