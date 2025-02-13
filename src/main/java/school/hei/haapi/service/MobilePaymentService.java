package school.hei.haapi.service;

import static school.hei.haapi.endpoint.rest.model.MobileMoneyType.ORANGE_MONEY;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.haapi.http.mapper.ExternalResponseMapper;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.model.MobileTransactionDetails;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.model.exception.ApiException;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.MobilePaymentRepository;
import school.hei.haapi.repository.MobileTransactionDetailsRepository;
import school.hei.haapi.service.mobileMoney.MobileMoneyApi;

@Service
@AllArgsConstructor
public class MobilePaymentService implements MobilePaymentRepository {
  private final MobileTransactionDetailsRepository mobileTransactionDetailsRepository;
  private final ExternalResponseMapper externalResponseMapper;
  private final MobileMoneyApi mobileMoneyApi;

  @Override
  public TransactionDetails findTransactionByMpbs(Mpbs mpbs) throws ApiException {
    String transactionRef = mpbs.getPspId();
    return externalResponseMapper.toExternalTransactionDetails(findTransactionById(transactionRef));
  }

  @Override
  public List<TransactionDetails> fetchThenSaveTransactionDetails() {
    // TODO: for each mobile money type fetch transaction
    return mobileMoneyApi.fetchThenSaveTransactionsDetails(ORANGE_MONEY);
  }

  public Optional<MobileTransactionDetails> findTransactionByMpbsWithoutException(Mpbs mpbs) {
    String transactionRef = mpbs.getPspId();
    return findTransactionByIdWithoutException(transactionRef);
  }

  public MobileTransactionDetails findTransactionById(String pspId) {
    return mobileTransactionDetailsRepository
        .findByPspTransactionRef(pspId)
        .orElseThrow(
            () -> new NotFoundException("Mobile transaction with ref." + pspId + " not found"));
  }

  public Optional<MobileTransactionDetails> findTransactionByIdWithoutException(String pspId) {
    return mobileTransactionDetailsRepository.findByPspTransactionRef(pspId);
  }

  public List<MobileTransactionDetails> saveAll(
      List<MobileTransactionDetails> mobileTransactionDetails) {
    return mobileTransactionDetailsRepository.saveAll(mobileTransactionDetails);
  }
}
