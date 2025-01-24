package school.hei.haapi.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import school.hei.haapi.model.MobileTransactionDetails;

@Repository
public interface MobileTransactionDetailsRepository
    extends JpaRepository<MobileTransactionDetails, String> {
  Optional<MobileTransactionDetails> findByPspTransactionRef(String pspRef);

  List<MobileTransactionDetails> findAllByPspTransactionRefIn(List<String> pspRefs);
}
