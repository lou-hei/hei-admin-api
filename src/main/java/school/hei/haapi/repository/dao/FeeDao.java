package school.hei.haapi.repository.dao;

import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.UNPAID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    List<Predicate> predicates =
        getPredicate(root, query, status, studentRef, monthFrom, monthTo, isMpbs, builder);

    CriteriaBuilder.Case<Object> statusOrder =
        builder
            .selectCase()
            .when(builder.equal(root.get("status"), LATE), 1)
            .when(builder.equal(root.get("status"), UNPAID), 2)
            .when(builder.equal(root.get("status"), PAID), 3);

    query
        .where(predicates.toArray(new Predicate[0]))
        .orderBy(
            builder.asc(statusOrder),
            builder.desc(root.get("dueDatetime")),
            builder.asc(root.get("id")));

    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  private boolean isCriteriaEmpty(
      FeeStatusEnum status, String studentRef, Instant monthFrom, Instant monthTo, Boolean isMpbs) {
    return status == null
        && studentRef == null
        && monthFrom == null
        && monthTo == null
        && Boolean.FALSE.equals(isMpbs);
  }

  private List<Predicate> buildPredicates(
      CriteriaBuilder builder,
      Root<Fee> root,
      List<Predicate> predicates,
      FeeStatusEnum status,
      String studentRef,
      Instant monthFrom,
      Instant monthTo,
      Boolean isMpbs,
      CriteriaQuery<Fee> query) {
    if (status != null) {
      predicates.add(builder.equal(root.get("status"), status));
    }

    if (studentRef != null) {
      Join<Fee, User> join = root.join("student");
      predicates.add(builder.like(join.get("ref"), "%" + studentRef + "%"));
    }

    addDatePredicates(builder, root, predicates, monthFrom, monthTo);

    if (Boolean.TRUE.equals(isMpbs)) {
      Join<Fee, Mpbs> mpbsJoin = root.join("mpbs");
      predicates.add(builder.isNotNull(mpbsJoin));
      query.orderBy(builder.desc(mpbsJoin.get("creationDatetime")));
    }
    return predicates;
  }

  private List<Predicate> getPredicate(
      Root<Fee> root,
      CriteriaQuery<Fee> query,
      FeeStatusEnum status,
      String studentRef,
      Instant monthFrom,
      Instant monthTo,
      Boolean isMpbs,
      CriteriaBuilder builder) {
    List<Predicate> predicates = new ArrayList<>();
    if (isCriteriaEmpty(status, studentRef, monthFrom, monthTo, isMpbs)) {
      predicates.add(builder.equal(root.get("status"), LATE));
    } else {
      buildPredicates(
          builder, root, predicates, status, studentRef, monthFrom, monthTo, isMpbs, query);
    }
    return predicates;
  }

  private void addDatePredicates(
      CriteriaBuilder builder,
      Root<Fee> root,
      List<Predicate> predicates,
      Instant monthFrom,
      Instant monthTo) {

    if (monthFrom != null && monthTo != null) {
      predicates.add(builder.between(root.get("dueDatetime"), monthFrom, monthTo));
    } else if (monthFrom != null) {
      predicates.add(builder.greaterThanOrEqualTo(root.get("dueDatetime"), monthFrom));
    } else if (monthTo != null) {
      predicates.add(builder.lessThanOrEqualTo(root.get("dueDatetime"), monthTo));
    }
  }
}
