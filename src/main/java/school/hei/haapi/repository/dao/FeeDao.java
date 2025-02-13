package school.hei.haapi.repository.dao;

import static java.lang.Boolean.TRUE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.UNPAID;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.SUCCESS;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.FeeStatusEnum;
import school.hei.haapi.endpoint.rest.model.FeeTypeEnum;
import school.hei.haapi.endpoint.rest.model.MpbsStatus;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.model.FeesStats;

@Repository
@AllArgsConstructor
public class FeeDao {
  private final EntityManager entityManager;

  public List<Fee> getByCriteria(
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
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
        getPredicate(
            root,
            query,
            mpbsStatus,
            feeType,
            status,
            studentRef,
            monthFrom,
            monthTo,
            isMpbs,
            builder);

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

  public List<FeesStats> getStatByCriteria(
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
      FeeStatusEnum status,
      String studentRef,
      Instant monthFrom,
      Instant monthTo,
      Boolean isMpbs) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<FeesStats> query = builder.createQuery(FeesStats.class);
    Root<Fee> root = query.from(Fee.class);
    List<Predicate> predicates =
        getStatPredicate(
            root,
            query,
            mpbsStatus,
            feeType,
            status,
            studentRef,
            monthFrom,
            monthTo,
            isMpbs,
            builder);

    Subquery<Long> pendingSubquery = query.subquery(Long.class);
    Root<Mpbs> pendingRoot = pendingSubquery.from(Mpbs.class);
    pendingSubquery
        .select(builder.literal(1L))
        .where(
            builder.equal(pendingRoot.get("status"), MpbsStatus.PENDING),
            builder.equal(pendingRoot.get("fee"), root));

    Subquery<Long> successSubquery = query.subquery(Long.class);
    Root<Mpbs> successRoot = successSubquery.from(Mpbs.class);
    successSubquery
        .select(builder.literal(1L))
        .where(
            builder.equal(successRoot.get("status"), SUCCESS),
            builder.equal(successRoot.get("fee"), root));

    query
        .where(predicates.toArray(new Predicate[0]))
        .multiselect(
            builder.count(root),
            builder.sum(
                builder
                    .selectCase()
                    .when(builder.equal(root.get("status"), PAID), 1L)
                    .otherwise(0L)
                    .as(Long.class)),
            builder.sum(
                builder
                    .selectCase()
                    .when(builder.equal(root.get("status"), UNPAID), 1L)
                    .otherwise(0L)
                    .as(Long.class)),
            builder.sum(
                builder
                    .selectCase()
                    .when(builder.equal(root.get("status"), LATE), 1L)
                    .otherwise(0L)
                    .as(Long.class)),
            builder.sum(
                builder
                    .selectCase()
                    .when(builder.exists(pendingSubquery), 1L)
                    .otherwise(0L)
                    .as(Long.class)),
            builder.sum(
                builder
                    .selectCase()
                    .when(builder.exists(successSubquery), 1L)
                    .otherwise(0L)
                    .as(Long.class)),
            builder.sum(
                builder
                    .selectCase()
                    .when(builder.like(root.get("comment"), "%Frais mensuel%"), 1L)
                    .otherwise(0L)
                    .as(Long.class)),
            builder.sum(
                builder
                    .selectCase()
                    .when(builder.like(root.get("comment"), "%Frais annuel%"), 1L)
                    .otherwise(0L)
                    .as(Long.class)));

    return entityManager.createQuery(query).getResultList();
  }

  private boolean isCriteriaEmpty(
      FeeStatusEnum status, String studentRef, Instant monthFrom, Instant monthTo, Boolean isMpbs) {
    return status == null
        && studentRef == null
        && monthFrom == null
        && monthTo == null
        && Boolean.FALSE.equals(isMpbs);
  }

  private List<Predicate> buildStatPredicates(
      CriteriaBuilder builder,
      Root<Fee> root,
      List<Predicate> predicates,
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
      FeeStatusEnum status,
      String studentRef,
      Instant monthFrom,
      Instant monthTo,
      Boolean isMpbs,
      CriteriaQuery<FeesStats> query) {
    if (feeType != null) {
      predicates.add(builder.equal(root.get("type"), feeType));
    }

    if (status != null) {
      predicates.add(builder.equal(root.get("status"), status));
    }

    if (studentRef != null) {
      Join<Fee, User> join = root.join("student");
      predicates.add(builder.like(join.get("ref"), "%" + studentRef + "%"));
    }

    addDatePredicates(builder, root, predicates, monthFrom, monthTo);

    if (TRUE.equals(isMpbs)) {
      Join<Fee, Mpbs> mpbsJoin = root.join("mpbs");
      predicates.add(builder.isNotNull(mpbsJoin));
    }

    if (mpbsStatus != null) {
      Join<Fee, Mpbs> mpbsJoin = root.join("mpbs");
      predicates.add(builder.equal(mpbsJoin.get("status"), mpbsStatus));
    }
    return predicates;
  }

  private List<Predicate> buildPredicates(
      CriteriaBuilder builder,
      Root<Fee> root,
      List<Predicate> predicates,
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
      FeeStatusEnum status,
      String studentRef,
      Instant monthFrom,
      Instant monthTo,
      Boolean isMpbs,
      CriteriaQuery<Fee> query) {
    if (feeType != null) {
      predicates.add(builder.equal(root.get("type"), feeType));
    }

    if (status != null) {
      predicates.add(builder.equal(root.get("status"), status));
    }

    if (studentRef != null) {
      Join<Fee, User> join = root.join("student");
      predicates.add(builder.like(join.get("ref"), "%" + studentRef + "%"));
    }

    addDatePredicates(builder, root, predicates, monthFrom, monthTo);

    if (TRUE.equals(isMpbs)) {
      Join<Fee, Mpbs> mpbsJoin = root.join("mpbs");
      predicates.add(builder.isNotNull(mpbsJoin));
      query.orderBy(builder.desc(mpbsJoin.get("creationDatetime")));
    }

    if (mpbsStatus != null) {
      Join<Fee, Mpbs> mpbsJoin = root.join("mpbs");
      predicates.add(builder.equal(mpbsJoin.get("status"), mpbsStatus));
    }
    return predicates;
  }

  private List<Predicate> getStatPredicate(
      Root<Fee> root,
      CriteriaQuery<FeesStats> query,
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
      FeeStatusEnum status,
      String studentRef,
      Instant monthFrom,
      Instant monthTo,
      Boolean isMpbs,
      CriteriaBuilder builder) {
    List<Predicate> predicates = new ArrayList<>();
    buildStatPredicates(
        builder,
        root,
        predicates,
        mpbsStatus,
        feeType,
        status,
        studentRef,
        monthFrom,
        monthTo,
        isMpbs,
        query);

    return predicates;
  }

  private List<Predicate> getPredicate(
      Root<Fee> root,
      CriteriaQuery<Fee> query,
      MpbsStatus mpbsStatus,
      FeeTypeEnum feeType,
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
          builder,
          root,
          predicates,
          mpbsStatus,
          feeType,
          status,
          studentRef,
          monthFrom,
          monthTo,
          isMpbs,
          query);
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

  public List<Fee> findAllByStatusAndDueDatetimeBetween(
      FeeStatusEnum status, Instant startDate, Instant endDate) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Fee> query = builder.createQuery(Fee.class);
    Root<Fee> root = query.from(Fee.class);

    List<Predicate> predicates = new ArrayList<>();

    if (status != null) {
      predicates.add(builder.equal(root.get("status"), status));
    }
    if (startDate != null) {
      predicates.add(builder.greaterThanOrEqualTo(root.get("dueDatetime"), startDate));
    }
    if (endDate != null) {
      predicates.add(builder.lessThanOrEqualTo(root.get("dueDatetime"), endDate));
    }

    query.where(predicates.toArray(new Predicate[0]));

    query.orderBy(builder.desc(root.get("dueDatetime")));

    return entityManager.createQuery(query).getResultList();
  }
}
