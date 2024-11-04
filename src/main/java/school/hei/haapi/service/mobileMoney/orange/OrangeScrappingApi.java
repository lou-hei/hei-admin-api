package school.hei.haapi.service.mobileMoney.orange;

import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static school.hei.haapi.service.utils.InstantUtils.getYesterday;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.MobileMoneyType;
import school.hei.haapi.http.mapper.ExternalExceptionMapper;
import school.hei.haapi.http.mapper.ExternalResponseMapper;
import school.hei.haapi.http.model.OrangeDailyTransactionScrappingDetails;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.model.MobileTransactionDetails;
import school.hei.haapi.model.exception.ApiException;
import school.hei.haapi.model.exception.NotFoundException;
import school.hei.haapi.repository.MobileTransactionDetailsRepository;
import school.hei.haapi.service.mobileMoney.MobileMoneyApi;

@Component("OrangeScrappingApi")
@Slf4j
class OrangeScrappingApi implements MobileMoneyApi {
  private final ObjectMapper objectMapper;
  private final ExternalExceptionMapper exceptionMapper;
  private final ExternalResponseMapper responseMapper;
  private final MobileTransactionDetailsRepository mobileTransactionDetailsRepository;
  private final String baseUrl;

  OrangeScrappingApi(
      ObjectMapper objectMapper,
      ExternalExceptionMapper exceptionMapper,
      ExternalResponseMapper responseMapper,
      MobileTransactionDetailsRepository mobileTransactionDetailsRepository,
      @Value("${ORANGE_SCRAPPER_BASEURL}") String baseUrl) {
    this.objectMapper = objectMapper;
    this.exceptionMapper = exceptionMapper;
    this.responseMapper = responseMapper;
    this.mobileTransactionDetailsRepository = mobileTransactionDetailsRepository;
    this.baseUrl = baseUrl;
  }

  @Override
  public List<TransactionDetails> fetchThenSaveTransactionsDetails(MobileMoneyType type) {
    String PATH = "/transactions?date=" + getYesterday();
    HttpRequest httpRequest =
        HttpRequest.newBuilder().uri(URI.create(baseUrl + PATH)).GET().build();
    OrangeDailyTransactionScrappingDetails a;

    try (HttpClient httpClient = HttpClient.newHttpClient()) {
      log.info("Fetching data from = {}", getYesterday());
      HttpResponse<String> response =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      log.info("Response = {}", response.toString());
      exceptionMapper.accept(response);
      log.info("No exception thrown");
      a = objectMapper.readValue(response.body(), OrangeDailyTransactionScrappingDetails.class);
      log.info(a.toString());
    } catch (IOException | InterruptedException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return mapTransactions(a);
  }

  private List<TransactionDetails> mapTransactions(OrangeDailyTransactionScrappingDetails a) {
    var mappedResponseList = a.getTransactions().stream().map(responseMapper::from).toList();

    // store the collected data ...
    var savedResponseList =
        mobileTransactionDetailsRepository.saveAll(
            mappedResponseList.stream()
                .map(responseMapper::toDomainMobileTransactionDetails)
                .toList());
    log.info(
        "Saved transactions = {}",
        savedResponseList.stream().map(MobileTransactionDetails::getPspTransactionRef).toList());

    return savedResponseList.stream().map(responseMapper::toRestMobileTransactionDetails).toList();
  }

  @Override
  public TransactionDetails getByTransactionRef(MobileMoneyType type, String ref)
      throws ApiException {
    return responseMapper.toExternalTransactionDetails(findTransactionById(ref));
  }

  private MobileTransactionDetails findTransactionById(String ref) {
    return mobileTransactionDetailsRepository
        .findByPspTransactionRef(ref)
        .orElseThrow(() -> new NotFoundException("Psp with id." + ref + " not found"));
  }
}
