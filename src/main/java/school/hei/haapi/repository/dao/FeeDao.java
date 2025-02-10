package school.hei.haapi.repository.dao;

import static jakarta.persistence.criteria.JoinType.LEFT;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNullElse;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.LATE;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PAID;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.PENDING;
import static school.hei.haapi.endpoint.rest.model.FeeStatusEnum.UNPAID;
import static school.hei.haapi.endpoint.rest.model.MpbsStatus.SUCCESS;
import static school.hei.haapi.endpoint.rest.model.PaymentFrequency.MONTHLY;
import static school.hei.haapi.endpoint.rest.model.PaymentFrequency.YEARLY;
import static school.hei.haapi.repository.dao.FeeDao.FeePayementType.BANK;
import static school.hei.haapi.repository.dao.FeeDao.FeePayementType.MPBS;
import static school.hei.haapi.repository.dao.FeeDao.FeeTypeWithWorkStudyEnum.REMEDIAL_COSTS;
import static school.hei.haapi.repository.dao.FeeDao.FeeTypeWithWorkStudyEnum.TUITION;
import static school.hei.haapi.repository.dao.FeeDao.FeeTypeWithWorkStudyEnum.WORK_STUDY_FEES;
import static school.hei.haapi.repository.dao.FeeDao.Grade.L1;
import static school.hei.haapi.repository.dao.FeeDao.Grade.L2;
import static school.hei.haapi.repository.dao.FeeDao.Grade.L3;
import static school.hei.haapi.service.utils.DateUtils.getDefaultMonthRange;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.AdvancedFeesStatistics;
import school.hei.haapi.endpoint.rest.model.FeeStatusEnum;
import school.hei.haapi.endpoint.rest.model.FeeTypeEnum;
import school.hei.haapi.endpoint.rest.model.LateFeesStats;
import school.hei.haapi.endpoint.rest.model.MpbsStatus;
import school.hei.haapi.endpoint.rest.model.PaidFeesStats;
import school.hei.haapi.endpoint.rest.model.PaymentFrequency;
import school.hei.haapi.endpoint.rest.model.PendingFeesStats;
import school.hei.haapi.endpoint.rest.model.TotalExpectedFeesStats;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.User;
import school.hei.haapi.repository.model.FeesStats;

@Repository
@AllArgsConstructor
public class FeeDao {
  private final EntityManager entityManager;

  enum FeeTypeWithWorkStudyEnum {
    TUITION,
    HARDWARE,
    STUDENT_INSURANCE,
    REMEDIAL_COSTS,
    WORK_STUDY_FEES;
  }

  enum Grade {
    L1,
    L2,
    L3;
  }

  enum FeePayementType {
    BANK,
    MPBS,
  }

  public AdvancedFeesStatistics getAdvancedFeesStatistics(Instant monthFrom, Instant monthTo) {
    LateFeesStats lateFeesStats =
        new LateFeesStats()
            .monthlyFees(countFeeByCriteria(null, null, LATE, monthFrom, monthTo, MONTHLY, null))
            .yearlyFees(countFeeByCriteria(null, null, LATE, monthFrom, monthTo, YEARLY, null))
            .remedialFees(
                BigDecimal.valueOf(
                    countFeeByCriteria(null, REMEDIAL_COSTS, LATE, monthFrom, monthTo, null, null)))
            .workStudyFees(
                countFeeByCriteria(null, WORK_STUDY_FEES, LATE, monthFrom, monthTo, null, null))
            .firstYear(countFeeByCriteria(L1, null, LATE, monthFrom, monthTo, null, null))
            .secondYear(countFeeByCriteria(L2, null, LATE, monthFrom, monthTo, null, null))
            .thirdYear(countFeeByCriteria(L3, null, LATE, monthFrom, monthTo, null, null));

    PendingFeesStats pendingFeesStats =
        new PendingFeesStats()
            .monthlyFees(countFeeByCriteria(null, null, PENDING, monthFrom, monthTo, MONTHLY, null))
            .yearlyFees(countFeeByCriteria(null, null, PENDING, monthFrom, monthTo, YEARLY, null))
            .remedialFees(
                BigDecimal.valueOf(
                    countFeeByCriteria(
                        null, REMEDIAL_COSTS, PENDING, monthFrom, monthTo, null, null)))
            .workStudyFees(
                countFeeByCriteria(null, WORK_STUDY_FEES, PENDING, monthFrom, monthTo, null, null))
            .firstYear(countFeeByCriteria(L1, null, PENDING, monthFrom, monthTo, null, null))
            .secondYear(countFeeByCriteria(L2, null, PENDING, monthFrom, monthTo, null, null))
            .thirdYear(countFeeByCriteria(L3, null, PENDING, monthFrom, monthTo, null, null));

    PaidFeesStats paidFeesStats =
        new PaidFeesStats()
            .monthlyFees(countFeeByCriteria(null, null, PAID, monthFrom, monthTo, MONTHLY, null))
            .yearlyFees(countFeeByCriteria(null, null, PAID, monthFrom, monthTo, YEARLY, null))
            .remedialFees(
                BigDecimal.valueOf(
                    countFeeByCriteria(null, REMEDIAL_COSTS, PAID, monthFrom, monthTo, null, null)))
            .workStudyFees(
                countFeeByCriteria(null, WORK_STUDY_FEES, PAID, monthFrom, monthTo, null, null))
            .firstYear(countFeeByCriteria(L1, null, PAID, monthFrom, monthTo, null, null))
            .secondYear(countFeeByCriteria(L2, null, PAID, monthFrom, monthTo, null, null))
            .thirdYear(countFeeByCriteria(L3, null, PAID, monthFrom, monthTo, null, null))
            .mobileMoneyFees(
                BigDecimal.valueOf(
                    countFeeByCriteria(null, null, PAID, monthFrom, monthTo, null, MPBS)))
            .bankFees(
                BigDecimal.valueOf(
                    countFeeByCriteria(null, null, PAID, monthFrom, monthTo, null, BANK)));

    TotalExpectedFeesStats totalExpectedFeesStats =
        new TotalExpectedFeesStats()
            .monthlyFees(countFeeByCriteria(null, null, null, monthFrom, monthTo, MONTHLY, null))
            .yearlyFees(countFeeByCriteria(null, null, null, monthFrom, monthTo, YEARLY, null))
            .workStudyFees(
                countFeeByCriteria(null, WORK_STUDY_FEES, null, monthFrom, monthTo, null, null))
            .firstYear(countFeeByCriteria(L1, null, null, monthFrom, monthTo, null, null))
            .secondYear(countFeeByCriteria(L2, null, null, monthFrom, monthTo, null, null))
            .thirdYear(countFeeByCriteria(L3, null, null, monthFrom, monthTo, null, null));

    return new AdvancedFeesStatistics()
        .lateFeesStats(lateFeesStats)
        .paidFeesStats(paidFeesStats)
        .pendingFeesStats(pendingFeesStats)
        .totalExpectedFeesStats(totalExpectedFeesStats);
  }

  private Long countFeeByCriteria(
      Grade grade,
      FeeTypeWithWorkStudyEnum feeType,
      FeeStatusEnum feeStatus,
      Instant monthFrom,
      Instant monthTo,
      PaymentFrequency frequencyType,
      FeePayementType paymentType) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = builder.createQuery(Long.class);
    Root<Fee> root = query.from(Fee.class);

    List<Predicate> predicates = new ArrayList<>();
    FeeTypeWithWorkStudyEnum feeTypeEnum = requireNonNullElse(feeType, TUITION);
    if (feeTypeEnum == WORK_STUDY_FEES) {
      predicates.add(builder.like(builder.lower(root.get("comment")), "%alternance"));
    } else {
      predicates.add(builder.equal(root.get("type"), FeeTypeEnum.valueOf(feeTypeEnum.toString())));
    }

    if (grade != null) {
      switch (grade) {
        case L1 -> predicates.add(builder.like(builder.lower(root.get("comment")), "%l1%"));
        case L2 -> predicates.add(builder.like(builder.lower(root.get("comment")), "%l2%"));
        case L3 -> predicates.add(builder.like(builder.lower(root.get("comment")), "%l3%"));
      }
    }

    if (feeStatus != null) {
      predicates.add(builder.equal(root.get("status"), feeStatus));
    }

    Instant[] defaultMonthRange = getDefaultMonthRange(monthFrom, monthTo);
    Instant startOfMonth = defaultMonthRange[0];
    Instant endOfMonth = defaultMonthRange[1];

    monthFrom = requireNonNullElse(monthFrom, startOfMonth);
    monthTo = requireNonNullElse(monthTo, endOfMonth);
    predicates.add(builder.between(root.get("dueDatetime"), monthFrom, monthTo));

    if (frequencyType != null) {
      switch (frequencyType) {
        case YEARLY -> predicates.add(builder.like(builder.lower(root.get("comment")), "%annuel%"));
        case MONTHLY ->
            predicates.add(builder.like(builder.lower(root.get("comment")), "%mensuel%"));
      }
    }

    if (paymentType != null) {
      switch (paymentType) {
        case BANK -> {
          Join<Fee, Mpbs> mpbsJoin = root.join("mpbs", LEFT);
          predicates.add(builder.isNull(mpbsJoin.get("id")));
        }
        case MPBS -> {
          Join<Fee, Mpbs> mpbsJoin = root.join("mpbs");
          predicates.add(builder.equal(mpbsJoin.get("id"), root.get("mpbs").get("id")));
        }
      }
    }

    query.where(predicates.toArray(new Predicate[0])).select(builder.count(root).as(Long.class));
    return entityManager.createQuery(query).getSingleResult();
  }

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
