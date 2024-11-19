package school.hei.haapi.repository.dao;

import static jakarta.persistence.criteria.JoinType.INNER;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.LetterStatus;
import school.hei.haapi.model.Fee;
import school.hei.haapi.model.Letter;
import school.hei.haapi.model.User;

@Repository
@AllArgsConstructor
public class LetterDao {

  private final EntityManager entityManager;

  public List<Letter> findByCriteria(
      String ref,
      String studentRef,
      LetterStatus status,
      String name,
      String feeId,
      Boolean isLinkedWithFee,
      Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Letter> query = builder.createQuery(Letter.class);
    Root<Letter> root = query.from(Letter.class);
    Join<Letter, User> userJoin = root.join("student", INNER);
    List<Predicate> predicates = new ArrayList<>();

    if (studentRef != null) {
      predicates.add(
          builder.or(
              builder.like(builder.lower(userJoin.get("ref")), "%" + studentRef + "%"),
              builder.like(userJoin.get("ref"), "%" + studentRef + "%")));
    }

    if (name != null) {
      predicates.add(
          builder.or(
              builder.like(builder.lower(userJoin.get("firstName")), "%" + name + "%"),
              builder.like(userJoin.get("firstName"), "%" + name + "%"),
              builder.like(builder.lower(userJoin.get("lastName")), "%" + name + "%"),
              builder.like(userJoin.get("lastName"), "%" + name + "%")));
    }

    if (isLinkedWithFee != null) {
      predicates.add(
          isLinkedWithFee ? builder.isNotNull(root.get("fee")) : builder.isNull(root.get("fee")));
    }

    if (feeId != null) {
      Join<Letter, Fee> feeJoin = root.join("fee", INNER);
      predicates.add(builder.equal(feeJoin.get("id"), feeId));
    }

    if (ref != null) {
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get("ref")), "%" + ref + "%"),
              builder.like(root.get("ref"), "%" + ref + "%")));
    }

    if (status != null) {
      predicates.add(builder.equal(root.get("status"), status));
    }

    if (!predicates.isEmpty()) {
      query.where(predicates.toArray(new Predicate[0])).distinct(true);
    }

    query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

    return entityManager
        .createQuery(query)
        .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }
}
