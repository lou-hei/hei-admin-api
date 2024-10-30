package school.hei.haapi.http.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import school.hei.haapi.http.model.OrangeTransactionDetails;
import school.hei.haapi.http.model.OrangeTransactionScrappingDetails;
import school.hei.haapi.http.model.TransactionDetails;
import school.hei.haapi.model.MobileTransactionDetails;

@Component
@AllArgsConstructor
@Slf4j
public class ExternalResponseMapper {

  public TransactionDetails from(OrangeTransactionDetails orangeRest) {
    return TransactionDetails.builder()
        .pspDatetimeTransactionCreation(orangeRest.getTransactionCreation())
        .pspTransactionRef(orangeRest.getTransactionRef())
        .pspTransactionAmount(orangeRest.getTransactionAmount())
        .build();
  }

  public TransactionDetails from(OrangeTransactionScrappingDetails orangeScrappingRest) {
    return TransactionDetails.builder()
        .pspDatetimeTransactionCreation(
            formatAndGetDateOfTransaction(orangeScrappingRest.getDate()))
        .pspOwnDatetimeVerification(
            formatPspOwnDatetimeVerification(
                orangeScrappingRest.getDate(), orangeScrappingRest.getTime()))
        .pspTransactionAmount(orangeScrappingRest.getAmount())
        .pspTransactionRef(orangeScrappingRest.getRef())
        .status(orangeScrappingRest.getStatusAsMpbsStatus())
        .build();
  }

  public TransactionDetails toExternalTransactionDetails(
      MobileTransactionDetails transactionDetails) {
    log.info("mapping transaction");
    return TransactionDetails.builder()
        .pspDatetimeTransactionCreation(transactionDetails.getPspDatetimeTransactionCreation())
        .pspTransactionRef(transactionDetails.getPspTransactionRef())
        .pspTransactionAmount(transactionDetails.getPspTransactionAmount())
        .status(transactionDetails.getStatus())
        .build();
  }

  public MobileTransactionDetails toDomainMobileTransactionDetails(
      TransactionDetails transactionDetails) {
    return MobileTransactionDetails.builder()
        .pspTransactionRef(transactionDetails.getPspTransactionRef())
        .pspTransactionAmount(transactionDetails.getPspTransactionAmount())
        .pspDatetimeTransactionCreation(transactionDetails.getPspDatetimeTransactionCreation())
        .pspOwnDatetimeVerification(transactionDetails.getPspOwnDatetimeVerification())
        .status(transactionDetails.getStatus())
        .build();
  }

  public TransactionDetails toRestMobileTransactionDetails(
      MobileTransactionDetails mobileTransactionDetails) {
    return TransactionDetails.builder()
        .pspTransactionAmount(mobileTransactionDetails.getPspTransactionAmount())
        .pspTransactionRef(mobileTransactionDetails.getPspTransactionRef())
        .pspDatetimeTransactionCreation(
            mobileTransactionDetails.getPspDatetimeTransactionCreation())
        .status(mobileTransactionDetails.getStatus())
        .build();
  }

  private Instant formatAndGetDateOfTransaction(String dateString) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    if (!Objects.isNull(dateString)) {
      LocalDate localDate = LocalDate.parse(dateString, formatter);
      return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
    }
    return null;
  }

  private Instant formatPspOwnDatetimeVerification(String dateString, String timeString) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    if (!Objects.isNull(timeString) && !Objects.isNull(dateString)) {
      String datetime = dateString + " " + timeString;

      LocalDateTime localDateTime = LocalDateTime.parse(datetime, formatter);
      log.info("mapped datetime: {}", localDateTime.toString());
      return localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }
    return formatAndGetDateOfTransaction(dateString);
  }
}
