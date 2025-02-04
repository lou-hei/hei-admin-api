package school.hei.haapi.repository.dao;

import static jakarta.persistence.criteria.JoinType.LEFT;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.AttendanceStatus;
import school.hei.haapi.model.Event;
import school.hei.haapi.model.EventParticipant;
import school.hei.haapi.model.Group;
import school.hei.haapi.model.User;

@Repository
@AllArgsConstructor
public class EventParticipantDao {
  private final EntityManager entityManager;

  public List<EventParticipant> findByCriteria(
      String eventId,
      Pageable pageable,
      String groupRef,
      String name,
      String ref,
      AttendanceStatus attendanceStatus) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<EventParticipant> query = builder.createQuery(EventParticipant.class);
    Root<EventParticipant> root = query.from(EventParticipant.class);
    Join<EventParticipant, User> userJoin = root.join("participant", LEFT);
    Join<EventParticipant, Event> eventJoin = root.join("event", LEFT);

    List<Predicate> predicates = new ArrayList<>();

    if (eventId != null) {
      predicates.add(builder.equal(eventJoin.get("id"), eventId));
    }

    if (groupRef != null) {
      Join<EventParticipant, Group> groupJoin = root.join("group", LEFT);
      predicates.add(builder.equal(groupJoin.get("ref"), groupRef));
    }

    if (name != null) {
      predicates.add(
          builder.and(
              builder.or(
                  builder.or(
                      builder.like(
                          builder.lower(userJoin.get("firstName")), "%" + name.toLowerCase() + "%"),
                      builder.like(userJoin.get("firstName"), "%" + name + "%")),
                  builder.or(
                      builder.like(
                          builder.lower(userJoin.get("lastName")), "%" + name.toLowerCase() + "%"),
                      builder.like(userJoin.get("lastName"), "%" + name + "%")))));
    }

    if (ref != null) {
      predicates.add(
          builder.like(builder.lower(userJoin.get("ref")), "%" + ref.toLowerCase() + "%"));
    }

    if (!predicates.isEmpty()) {
      query.where(predicates.toArray(new Predicate[0]));
    }

    if (attendanceStatus != null) {
      predicates.add(builder.equal(root.get("status"), attendanceStatus.toString()));
    }

    if (pageable != null) {
      query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

      return entityManager
          .createQuery(query)
          .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
          .setMaxResults(pageable.getPageSize())
          .getResultList();
    }

    return entityManager.createQuery(query).getResultList();
  }
}
