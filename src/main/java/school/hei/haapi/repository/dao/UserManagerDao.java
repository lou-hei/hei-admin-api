package school.hei.haapi.repository.dao;

import static jakarta.persistence.criteria.JoinType.LEFT;
import static school.hei.haapi.endpoint.rest.model.WorkStudyStatus.HAVE_BEEN_WORKING;
import static school.hei.haapi.endpoint.rest.model.WorkStudyStatus.WILL_BE_WORKING;
import static school.hei.haapi.endpoint.rest.model.WorkStudyStatus.WORKING;
import static school.hei.haapi.model.GroupFlow.GroupFlowType.JOIN;
import static school.hei.haapi.model.GroupFlow.GroupFlowType.LEAVE;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.WorkStudyStatus;
import school.hei.haapi.model.*;

@Repository
@AllArgsConstructor
public class UserManagerDao {
  private EntityManager entityManager;

  public List<User> findByCriteria(
      User.Role role,
      String ref,
      String firstName,
      String lastName,
      Pageable pageable,
      List<User.Status> status,
      User.Sex sex,
      WorkStudyStatus workStatus,
      Instant commitmentBeginDate,
      String courseId,
      Instant commitmentComparison,
      List<String> excludeGroupIds) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> query = builder.createQuery(User.class);
    Root<User> root = query.from(User.class);
    Join<User, WorkDocument> workDocumentJoin = null;
    Predicate predicate = builder.conjunction();

    Predicate hasUserRef =
        builder.or(
            builder.like(builder.lower(root.get("ref")), "%" + ref + "%"),
            builder.like(root.get("ref"), "%" + ref + "%"));

    Predicate hasUserFirstName =
        builder.or(
            builder.like(builder.lower(root.get("firstName")), "%" + firstName + "%"),
            builder.like(root.get("firstName"), "%" + firstName + "%"));

    Predicate hasUserLastName =
        builder.or(
            builder.like(builder.lower(root.get("lastName")), "%" + lastName + "%"),
            builder.like(root.get("lastName"), "%" + lastName + "%"));

    Predicate hasUserRole = builder.equal(root.get("role"), role);

    if (courseId != null && !courseId.isEmpty() && !courseId.isBlank()) {
      Join<User, AwardedCourse> awardedCourseJoin = root.join("awardedCourses", LEFT);
      Join<AwardedCourse, Course> courseJoin = awardedCourseJoin.join("course", LEFT);
      Expression<String> courseIdExpression = courseJoin.get("id");
      predicate = builder.and(predicate, builder.equal(courseIdExpression, courseId));
    }

    if (commitmentBeginDate != null) {
      workDocumentJoin = root.join("workDocuments", LEFT);
      Expression<Instant> commitmentBeginExpression = workDocumentJoin.get("commitmentBegin");
      predicate =
          builder.and(
              predicate,
              builder.greaterThanOrEqualTo(commitmentBeginExpression, commitmentBeginDate));
    }

    if (firstName != null && !firstName.isEmpty()) {
      predicate = builder.and(predicate, hasUserFirstName);
    }

    if (status != null) {
      predicate = builder.and(predicate, root.get("status").in(status));
    }

    if (sex != null) {
      predicate = builder.and(predicate, builder.equal(root.get("sex"), sex));
    }

    if (WORKING.equals(workStatus)) {
      workDocumentJoin = root.join("workDocuments", LEFT);
      predicate =
          builder.and(
              predicate,
              builder.lessThanOrEqualTo(
                  workDocumentJoin.get("commitmentBegin"), commitmentComparison));
    }
    if (HAVE_BEEN_WORKING.equals(workStatus)) {
      workDocumentJoin = root.join("workDocuments", LEFT);
      predicate =
          builder.and(
              predicate,
              builder.lessThanOrEqualTo(
                  workDocumentJoin.get("commitmentEnd"), commitmentComparison));
    }
    if (WILL_BE_WORKING.equals(workStatus)) {
      workDocumentJoin = root.join("workDocuments", LEFT);
      predicate =
          builder.and(
              predicate,
              builder.greaterThanOrEqualTo(
                  workDocumentJoin.get("commitmentBegin"), commitmentComparison));
    }

    if (excludeGroupIds != null) {
      Subquery<String> subquery = query.subquery(String.class);
      Root<GroupFlow> groupFlowRoot = subquery.from(GroupFlow.class);

      subquery.select(groupFlowRoot.get("student").get("id"));
      //      subquery.where(builder.like(groupFlowRoot.get("group").get("id"), excludeGroupId));
      subquery.where(groupFlowRoot.get("group").get("id").in(excludeGroupIds));

      subquery.groupBy(
          groupFlowRoot.get("group").get("id"), groupFlowRoot.get("student").get("id"));

      Expression<Integer> joinCount =
          builder.sum(
              builder
                  .<Integer>selectCase()
                  .when(builder.equal(groupFlowRoot.get("groupFlowType"), JOIN), 1)
                  .otherwise(0));

      Expression<Integer> leaveCount =
          builder.sum(
              builder
                  .<Integer>selectCase()
                  .when(builder.equal(groupFlowRoot.get("groupFlowType"), LEAVE), 1)
                  .otherwise(0));

      subquery.having(builder.greaterThan(joinCount, leaveCount));

      predicate = builder.and(predicate, builder.not(root.get("id").in(subquery)));
    }

    predicate = builder.and(predicate, hasUserRole, hasUserRef, hasUserLastName);

    query.where(predicate).orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  public List<User> findByLinkedCourse(User.Role role, String courseId, Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> query = builder.createQuery(User.class);
    Root<User> root = query.from(User.class);
    Join<User, AwardedCourse> awardedCourse = root.join("awardedCourses");
    Join<AwardedCourse, Course> course = awardedCourse.join("course");

    Predicate hasCourseId = builder.equal(course.get("id"), courseId);
    Predicate hasUserRole = builder.equal(root.get("role"), role);
    query
        .distinct(true)
        .where(builder.and(hasUserRole, hasCourseId))
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  public void updateUserStatusById(User.Status status, String userId) {
    User user = entityManager.find(User.class, userId);
    if (user != null) {
      user.setStatus(status);
      entityManager.merge(user);
    }
  }
}
