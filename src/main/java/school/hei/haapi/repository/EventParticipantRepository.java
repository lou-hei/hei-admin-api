package school.hei.haapi.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import school.hei.haapi.endpoint.rest.model.AttendanceStatus;
import school.hei.haapi.model.EventParticipant;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, String> {
  boolean existsByEventIdAndGroupIdAndParticipantId(
      String eventId, String groupId, String participantId);

  Optional<List<EventParticipant>> findAllByEventId(String eventId, Pageable pageable);

  Optional<List<EventParticipant>> findAllByEventIdAndGroupRef(
      String eventId, String groupRef, Pageable pageable);

  Integer countByEventIdAndStatus(String eventId, AttendanceStatus status);
}
