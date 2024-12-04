package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_ID;
import static school.hei.haapi.integration.conf.TestUtils.MONITOR1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT2_ID;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT3_ID;
import static school.hei.haapi.integration.conf.TestUtils.TEACHER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.assertThrowsForbiddenException;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;
import static school.hei.haapi.integration.conf.TestUtils.setUpEventBridge;
import static school.hei.haapi.integration.conf.TestUtils.student1;
import static school.hei.haapi.integration.conf.TestUtils.student2;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.MonitoringApi;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.model.LinkStudentsByMonitorIdRequest;
import school.hei.haapi.endpoint.rest.model.Student;
import school.hei.haapi.integration.conf.FacadeITMockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
public class MonitoringStudentIT extends FacadeITMockedThirdParties {
  @MockBean private EventBridgeClient eventBridgeClientMock;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpEventBridge(eventBridgeClientMock);
  }

  @Test
  @Disabled("dirty")
  void monitor_follow_students_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    MonitoringApi api = new MonitoringApi(manager1Client);

    // 1. Link some students to a monitor ...
    List<Student> studentsLinked =
        api.linkStudentsByMonitorId(
            MONITOR1_ID, new LinkStudentsByMonitorIdRequest().studentsIds(List.of(STUDENT1_ID)));

    assertEquals(1, studentsLinked.size());
    assertTrue(studentsLinked.contains(student1()));

    List<Student> studentsLinkedByMonitor =
        api.linkStudentsByMonitorId(
            MONITOR1_ID, new LinkStudentsByMonitorIdRequest().studentsIds(List.of(STUDENT2_ID)));

    assertEquals(1, studentsLinkedByMonitor.size());
    assertTrue(studentsLinkedByMonitor.contains(student2()));

    // 2. ... Except that the monitor access to his resources ...
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    PayingApi payingApi = new PayingApi(monitor1Client);
    List<Fee> followedStudentFee = payingApi.getStudentFees(STUDENT2_ID, 1, 10, null);

    assertFalse(followedStudentFee.isEmpty());

    // 3. ... And except that for other student monitor doesn't have access
    assertThrowsForbiddenException(() -> payingApi.getStudentFees(STUDENT3_ID, 1, 10, null));
  }

  @Test
  void manager_read_students_followed_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    MonitoringApi api = new MonitoringApi(manager1Client);

    List<Student> studentsLinkedToAMonitor = api.getLinkedStudentsByMonitorId(MONITOR1_ID, 1, 10);
    assertEquals(1, studentsLinkedToAMonitor.size());
    assertEquals(student1(), studentsLinkedToAMonitor.getFirst());
  }

  @Test
  void monitor_read_own_followed_students_ok() throws ApiException {
    ApiClient monitor1Client = anApiClient(MONITOR1_TOKEN);
    MonitoringApi api = new MonitoringApi(monitor1Client);

    List<Student> studentsLinkedToAMonitor = api.getLinkedStudentsByMonitorId(MONITOR1_ID, 1, 10);
    assertEquals(1, studentsLinkedToAMonitor.size());
    assertEquals(student1(), studentsLinkedToAMonitor.getFirst());
  }

  @Test
  void monitor_follow_students_ko() throws ApiException {
    ApiClient teacher1client = anApiClient(TEACHER1_TOKEN);
    MonitoringApi teacherApi = new MonitoringApi(teacher1client);

    ApiClient student1client = anApiClient(STUDENT1_TOKEN);
    MonitoringApi studentApi = new MonitoringApi(student1client);

    assertThrowsForbiddenException(
        () ->
            teacherApi.linkStudentsByMonitorId(
                MONITOR1_ID, someStudentsIdentifierToLinkToAMonitor()));
    assertThrowsForbiddenException(
        () ->
            studentApi.linkStudentsByMonitorId(
                MONITOR1_ID, someStudentsIdentifierToLinkToAMonitor()));
  }

  public static LinkStudentsByMonitorIdRequest someStudentsIdentifierToLinkToAMonitor() {
    return new LinkStudentsByMonitorIdRequest().studentsIds(List.of(STUDENT1_ID, STUDENT2_ID));
  }
}
