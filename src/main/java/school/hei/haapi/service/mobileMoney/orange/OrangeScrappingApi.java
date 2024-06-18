package school.hei.haapi.service.mobileMoney.orange;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.MobileMoneyType;
import school.hei.haapi.http.mapper.ExternalExceptionMapper;
import school.hei.haapi.http.mapper.ExternalResponseMapper;
import school.hei.haapi.http.model.OrangeDailyTransactionScrappingDetails;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.model.exception.ApiException;
import school.hei.haapi.service.mobileMoney.MobileMoneyApi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static school.hei.haapi.service.utils.InstantUtils.getYesterday;

@Component("OrangeScrappingApi")
@AllArgsConstructor
class OrangeScrappingApi implements MobileMoneyApi {
    private final ObjectMapper objectMapper;
    private final ExternalExceptionMapper exceptionMapper;
    private final ExternalResponseMapper responseMapper;

    private static final String BASE_URL = "";

    public List<TransactionDetails> scrapOrangeTransactions() throws ApiException {
        String PATH = "/transactions?date=" + getYesterday();

        try(HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + PATH))
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            exceptionMapper.accept(response);
            return objectMapper.readValue(response.body(), OrangeDailyTransactionScrappingDetails.class)
                    .getTransactions()
                    .stream()
                    .map(responseMapper::from)
                    .toList();
        } catch (IOException | InterruptedException e) {
            throw new ApiException(SERVER_EXCEPTION, e);
        }
    }

    @Override
    public TransactionDetails getByTransactionRef(MobileMoneyType type, String ref) throws ApiException {
        List<TransactionDetails> dailyTransactions = scrapOrangeTransactions();

        return dailyTransactions.stream()
                .filter(transactionDetails -> ref.equals(transactionDetails.getPspTransactionRef()))
                .distinct()
                .toList()
                .getFirst();
    }
}
