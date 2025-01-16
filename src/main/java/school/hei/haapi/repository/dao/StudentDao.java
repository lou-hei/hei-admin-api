package school.hei.haapi.repository.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.EventParticipant;
import school.hei.haapi.model.User;

@Repository
@AllArgsConstructor
public class StudentDao {
  private EntityManager entityManager;

  public List<User> getStudentsByCriteria(String eventId) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> query = builder.createQuery(User.class);
    Root<User> root = query.from(User.class);

    Join<User, EventParticipant> eventParticipant = root.join("event_participant");

    if (eventId != null) {
      query.where(builder.equal(eventParticipant.get("event_id"), eventId));
    }

    return entityManager.createQuery(query).getResultList();
  }
}
