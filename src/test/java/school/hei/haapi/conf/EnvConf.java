package school.hei.haapi.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {
  void configureProperties(DynamicPropertyRegistry registry) {
    String flywayTestdataPath = "classpath:/db/testdata";
    registry.add("env", () -> "test");
    registry.add("sentry.dsn", () -> "https://public@sentry.example.com/1");
    registry.add("aws.ses.source", () -> "dummy");
    registry.add("aws.ses.contact", () -> "dummy");
    registry.add("aws.cognito.userPool.id", () -> "dummy");
    registry.add("OWNCLOUD_BASE_URL", () -> "https://owncloud.example.com");
    registry.add("ORANGE_SCRAPPER_BASEURL", () -> "https://scrapper.com");
    registry.add("OWNCLOUD_USERNAME", () -> "dummy");
    registry.add("OWNCLOUD_PASSWORD", () -> "dummy");
    registry.add("OWNCLOUD_PASSWORD", () -> "dummy");
    registry.add("spring.flyway.locations", () -> "classpath:/db/migration," + flywayTestdataPath);
  }
}
