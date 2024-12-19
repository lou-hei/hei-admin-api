package school.hei.haapi.repository.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.EventType;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.model.Event;

@Repository
@AllArgsConstructor
public class EventDao {
  private final EntityManager entityManager;

  public List<Event> findByCriteria(
          String title, Instant from, Instant to, EventType eventType, Group groupName, Pageable pageable) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Event> query = builder.createQuery(Event.class);
    Root<Event> root = query.from(Event.class);

    List<Predicate> predicates = new ArrayList<>();

    if (title != null) {
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get("title")), "%" + title + "%"),
              builder.like(root.get("title"), "%" + title + "%")));
    }
    if (from != null) {
      predicates.add(builder.greaterThanOrEqualTo(root.get("beginDatetime"), from));
    }

    if (to != null) {
      predicates.add(builder.lessThanOrEqualTo(root.get("beginDatetime"), to));
    }

    if (eventType != null) {
      predicates.add(builder.equal(root.get("type"), eventType));
    }

    if (groupName != null) {
      predicates.add(
              builder.equal(
                      root.join("groups").get("name"), groupName
              )
      );
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
