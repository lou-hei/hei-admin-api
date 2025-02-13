package school.hei.haapi.integration.conf;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  private static final String SENTRY_MOCK_DSN = "https://examplePublicKey@example.sentry.io/12345";

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    PostgreSQLContainer<?> postgresContainer =
        new PostgreSQLContainer<>("postgres:13.9")
            .withDatabaseName("it-db")
            .withUsername("sa")
            .withPassword("sa");
    postgresContainer.start();

    String flywayTestdataPath = "classpath:/db/testdata";
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        applicationContext,
        "sentry.dsn=" + SENTRY_MOCK_DSN,
        "env=test",
        "server.port=" + this.getServerPort(),
        "aws.cognito.userPool.id=eu-west-3_mGxK1Bi8s",
        "aws.eventBridge.bus=dummy",
        "aws.sqs.queue.url=dummy",
        "aws.ses.source=dummy",
        "aws.ses.contact=dummy",
        "test.aws.cognito.idToken=dummy",
        "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgresContainer.getUsername(),
        "spring.datasource.password=" + postgresContainer.getPassword(),
        "spring.flyway.locations=classpath:/db/migration," + flywayTestdataPath,
        "OWNCLOUD_BASE_URL=https://owncloud.example.com",
        "ORANGE_SCRAPPER_BASEURL=https://scrapper.com",
        "OWNCLOUD_USERNAME=dummy",
        "OWNCLOUD_PASSWORD=dummy",
        "CASDOOR_ORGANIZATION_NAME=dummy",
        "CASDOOR_REDIRECT_URL=dummy",
        "CASDOOR_FRONTEND_URL=dummy",
        "CASDOOR_ENDPOINT=dummy",
        "CASDOOR_CERTIFICATE=dummy",
        "CASDOOR_APPLICATION_NAME=dummy",
        "CASDOOR_CLIENT_ID=dummy",
        "CASDOOR_CLIENT_SECRET=dummy");
  }

  public abstract int getServerPort();
}
