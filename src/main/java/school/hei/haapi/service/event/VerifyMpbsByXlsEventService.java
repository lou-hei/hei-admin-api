package school.hei.haapi.service.event;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.SendVerifyMpbsByXlsEventEmail;
import school.hei.haapi.endpoint.event.model.VerifyMpbsByXlsEvent;
import school.hei.haapi.file.bucket.BucketComponent;
import school.hei.haapi.model.Mpbs.Mpbs;
import school.hei.haapi.service.MpbsVerificationService;

@AllArgsConstructor
@Service
@Slf4j
public class VerifyMpbsByXlsEventService implements Consumer<VerifyMpbsByXlsEvent> {
  private final MpbsVerificationService mpbsVerificationService;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;

  @Override
  public void accept(VerifyMpbsByXlsEvent event) {
    try {
      log.info("Verification started");
      List<Mpbs> mpbs =
          mpbsVerificationService.computeFromXls(bucketComponent.download(event.getFileKey()));
      eventProducer.accept(
          List.of(
              SendVerifyMpbsByXlsEventEmail.builder()
                  .count(mpbs.size())
                  .verificationInstant(event.getVerificationInstant())
                  .build()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
