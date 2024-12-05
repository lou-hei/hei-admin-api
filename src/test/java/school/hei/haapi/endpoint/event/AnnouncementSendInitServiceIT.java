package school.hei.haapi.endpoint.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.Scope.GLOBAL;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.event.model.AnnouncementSendInit;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.service.event.AnnouncementSendInitService;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AnnouncementSendInitServiceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AnnouncementSendInitServiceIT extends MockedThirdParties {
  @Autowired AnnouncementSendInitService announcementSendInitService;
  @MockBean EventProducer eventProducerMock;

  static AnnouncementSendInit announcement() {
    return AnnouncementSendInit.builder()
        .title("Title")
        .scope(GLOBAL)
        .id("test_id")
        .senderFullName("John Doe")
        .build();
  }

  @Test
  void should_invoke_eventproducer() {
    announcementSendInitService.accept(announcement());

    // 3 depends on actual data
    verify(eventProducerMock, times(3)).accept(any());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
