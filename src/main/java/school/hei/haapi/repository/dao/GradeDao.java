package school.hei.haapi.repository.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.Grade;

@Repository
@AllArgsConstructor
public class GradeDao {
  private final EntityManager entityManager;

  public List<Grade> getGradesByExamId(String examId, Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Grade> query = builder.createQuery(Grade.class);
    Root<Grade> root = query.from(Grade.class);

    query.where(builder.equal(root.get("exam").get("id"), examId));

    if (pageable == null) {
      return entityManager.createQuery(query).getResultList();
    }

    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }
}
