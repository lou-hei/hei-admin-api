package school.hei.haapi.integration.conf;

import org.junit.jupiter.api.function.Executable;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Group;
import school.hei.haapi.endpoint.rest.security.cognito.CognitoComponent;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static final String STUDENT1_ID = "student1_id";
  public static final String STUDENT2_ID = "student2_id";
  public static final String TEACHER1_ID = "teacher1_id";
  public static final String TEACHER2_ID = "teacher2_id";
  public static final String BAD_TOKEN = "bad_token"; // null, invalid or expired
  public static final String STUDENT1_TOKEN = "student1_token";
  public static final String TEACHER1_TOKEN = "teacher1_token";
  public static final String MANAGER1_TOKEN = "manager1_token";
  public static final String GROUP1_ID = "group1_id";

  public static ApiClient aClient(String token, int serverPort) {
    ApiClient client = new ApiClient();
    client.setScheme("http");
    client.setHost("localhost");
    client.setPort(serverPort);
    client.setRequestInterceptor(httpRequestBuilder ->
        httpRequestBuilder.header("Authorization", "Bearer " + token));
    return client;
  }

  public static void setUpCognito(CognitoComponent cognitoComponent) {
    when(cognitoComponent.findEmailByBearer(TestUtils.STUDENT1_TOKEN)).thenReturn("ryan@hei.school");
    when(cognitoComponent.findEmailByBearer(TestUtils.TEACHER1_TOKEN)).thenReturn("teacher1@hei.school");
    when(cognitoComponent.findEmailByBearer(TestUtils.MANAGER1_TOKEN)).thenReturn("manager1@hei.school");
  }

  public static void assertThrowsApiException(Executable executable, String expectedBody) {
    ApiException apiException = assertThrows(ApiException.class, executable);
    assertEquals(apiException.getResponseBody(), expectedBody);
  }

  public static boolean isValidUUID(String candidate) {
    try {
      UUID.fromString(candidate);
      return true;
    } catch (Exception e){
      return false;
    }
  }
}
