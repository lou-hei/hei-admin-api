package school.hei.haapi.endpoint.rest.security.cognito;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.haapi.file.bucket.BucketConf;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

class CognitoComponentTest {

  CognitoComponent cognitoComponent;
  CognitoIdentityProviderClient cognitoClient;
  @MockBean BucketConf bucketConf;

  @BeforeEach
  void setUp() {
    cognitoClient = mock(CognitoIdentityProviderClient.class);
    cognitoClient = mock(CognitoIdentityProviderClient.class);
    cognitoComponent = new CognitoComponent(mock(CognitoConf.class), cognitoClient);
  }

  @Test
  void new_user_is_created() {
    String newEmail = "test+" + randomUUID() + "@hei.school";
    when(cognitoClient.adminCreateUser((AdminCreateUserRequest) any()))
        .thenReturn(
            AdminCreateUserResponse.builder()
                .user(UserType.builder().username("username").build())
                .build());

    String cognitoUsername = cognitoComponent.createUser(newEmail);

    assertEquals("username", cognitoUsername);
  }
}
