package school.hei.haapi.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import school.hei.haapi.endpoint.rest.model.MpbsStatus;

@Entity
@Table(name = "\"mobile_transaction_details\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class MobileTransactionDetails implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private Integer pspTransactionAmount;

  private Instant pspDatetimeTransactionCreation;

  private Instant pspOwnDatetimeVerification;

  private String pspTransactionRef;

  @Column(name = "\"status\"")
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private MpbsStatus status;
}
