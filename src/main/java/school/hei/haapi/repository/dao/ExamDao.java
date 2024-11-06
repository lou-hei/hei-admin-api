package school.hei.haapi.repository.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.AwardedCourse;
import school.hei.haapi.model.Course;
import school.hei.haapi.model.Exam;
import school.hei.haapi.model.Group;

@Repository
@AllArgsConstructor
public class ExamDao {
  private final EntityManager entityManager;

  public List<Exam> findByCriteria(
      Pageable pageable,
      String title,
      String courseCode,
      String groupRef,
      Instant examinationDateStart,
      Instant examinationDateEnd,
      String awardedCourseId) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Exam> query = builder.createQuery(Exam.class);
    Root<Exam> root = query.from(Exam.class);
    ArrayList<Predicate> predicates = new ArrayList<>();

    if (title != null && !title.isEmpty()) {
      predicates.add(builder.like(root.get("title"), "%" + title.toLowerCase() + "%"));
    }

    Join<Exam, AwardedCourse> awardedCourseJoin = root.join("awardedCourse", JoinType.LEFT);
    if (courseCode != null && !courseCode.isEmpty()) {
      Join<AwardedCourse, Course> courseJoin = awardedCourseJoin.join("course", JoinType.LEFT);
      predicates.add(
          builder.like(
              builder.lower(courseJoin.get("code")), "%" + courseCode.toLowerCase() + "%"));
    }
    if (awardedCourseId != null && !awardedCourseId.isEmpty()) {
      predicates.add(builder.equal(awardedCourseJoin.get("id"), awardedCourseId));
    }
    if (groupRef != null && !groupRef.isEmpty()) {
      Join<AwardedCourse, Group> groupJoin = awardedCourseJoin.join("group", JoinType.LEFT);
      predicates.add(
          builder.like(builder.lower(groupJoin.get("ref")), "%" + groupRef.toLowerCase() + "%"));
    }
    addExaminationDateRangePredicates(
        examinationDateStart, examinationDateEnd, predicates, builder, root);
    query
        .distinct(true)
        .where(predicates.toArray(new Predicate[0]))
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));
    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  private static void addExaminationDateRangePredicates(
      Instant examinationDateStart,
      Instant examinationDateEnd,
      ArrayList<Predicate> predicates,
      CriteriaBuilder builder,
      Root<Exam> root) {
    if (examinationDateStart != null && examinationDateEnd != null) {
      predicates.add(
          builder.between(root.get("examinationDate"), examinationDateStart, examinationDateEnd));
    } else if (examinationDateStart == null && examinationDateEnd != null) {
      predicates.add(
          builder.between(root.get("examinationDate"), Instant.now(), examinationDateEnd));
    } else if (examinationDateEnd == null && examinationDateStart != null) {
      predicates.add(
          builder.between(root.get("examinationDate"), examinationDateStart, Instant.now()));
    }
  }
}
