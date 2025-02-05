package school.hei.haapi.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

  @Query("SELECT e FROM Event e WHERE e.beginDatetime BETWEEN :from AND :to")
  List<Event> findEventsBetweenInstant(Instant from, Instant to);
}
