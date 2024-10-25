package school.hei.haapi.repository.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.FeeStatusEnum;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.User;

@Repository
@AllArgsConstructor
public class FeeDao {

  private final EntityManager entityManager;

  public List<Fee> getByCriteria(
      FeeStatusEnum status,
      String studentRef,
      Instant monthFrom,
      Instant monthTo,
      Boolean isMpbs,
      Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Fee> query = builder.createQuery(Fee.class);
    Root<Fee> root = query.from(Fee.class);

    List<Predicate> predicates = new ArrayList<>();

    if (status == null
        && studentRef == null
        && monthFrom == null
        && monthTo == null
        && isMpbs == false) {
      predicates.add(builder.equal(root.get("status"), FeeStatusEnum.LATE));
    } else {
      if (status != null) {
        predicates.add(builder.equal(root.get("status"), status));
      }

      if (studentRef != null) {
        Join<Fee, User> join = root.join("student");
        predicates.add(builder.like(join.get("ref"), "%" + studentRef + "%"));
      }

      if (monthFrom != null && monthTo != null) {
        predicates.add(builder.between(root.get("dueDatetime"), monthFrom, monthTo));
      } else if (monthFrom != null) {
        predicates.add(builder.greaterThanOrEqualTo(root.get("dueDatetime"), monthFrom));
      } else if (monthTo != null) {
        predicates.add(builder.lessThanOrEqualTo(root.get("dueDatetime"), monthTo));
      }

      if (isMpbs != null) {
        if (isMpbs) {
          Join<Fee, Mpbs> mpbsJoin = root.join("mpbs");
          predicates.add(builder.isNotNull(mpbsJoin));
          query.orderBy(builder.desc(mpbsJoin.get("creationDatetime")));
        }
      }
    }

    query
        .where(predicates.toArray(new Predicate[0]))
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }
}
