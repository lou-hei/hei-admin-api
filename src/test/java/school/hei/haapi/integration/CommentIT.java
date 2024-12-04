package school.hei.haapi.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.endpoint.rest.model.OrderDirection.ASC;
import static school.hei.haapi.integration.StudentIT.student1;
import static school.hei.haapi.integration.conf.TestUtils.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.endpoint.rest.api.CommentsApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.Comment;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.MockedThirdParties;
import school.hei.haapi.integration.conf.TestUtils;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = CommentIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Disabled
class CommentIT extends MockedThirdParties {
  public static String STUDENT1_REF = "STD21001";

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpS3Service(fileService, student1());
  }

  @Test
  void manager_read_comments_by_student_ref_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    List<Comment> actual = api.getComments(1, 10, null, STUDENT1_REF);

    assertEquals(3, actual.size());
    assertTrue(actual.contains(comment1()));
  }

  @Test
  void teacher_read_comments_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    List<Comment> actualTimestampDescendant = api.getComments(1, 10, null, null);
    List<Comment> actualTimestampAscendant = api.getComments(1, 10, ASC, null);

    // Verify Comments are filter by timestamp Ascendant
    assertEquals(comment1().getContent(), actualTimestampAscendant.get(0).getContent());
    assertEquals(
        comment1().getCreationDatetime(), actualTimestampAscendant.get(0).getCreationDatetime());

    // Verify Comments are filter by timestamp Descendant (by default)
    assertEquals(
        createCommentByTeacher().getContent(), actualTimestampDescendant.get(0).getContent());
    assertEquals(
        createCommentByTeacher().getStudentId(),
        actualTimestampDescendant.get(0).getSubject().getId());
  }

  @Test
  void manager_read_comment_about_student1_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    List<Comment> actual = api.getStudentComments(STUDENT1_ID, null, 1, 15);

    assertTrue(actual.contains(comment1()));
    assertTrue(actual.contains(comment2()));
  }

  @Test
  void teacher_read_comment_about_student1_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    List<Comment> actual = api.getStudentComments(STUDENT1_ID, null, 1, 15);

    assertTrue(actual.contains(comment1()));
    assertTrue(actual.contains(comment2()));
  }

  @Test
  void student1_read_comment_about_him_ok() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    List<Comment> actual = api.getStudentComments(STUDENT1_ID, null, 1, 15);

    assertTrue(actual.contains(comment1()));
    assertTrue(actual.contains(comment2()));
  }

  @Test
  void student1_read_comment_about_other_student_ko() throws ApiException {
    ApiClient apiClient = anApiClient(STUDENT1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    assertThrowsForbiddenException(() -> api.getStudentComments(STUDENT2_ID, null, 1, 15));
  }

  @Test
  void manager_comment_about_student1_ok() throws ApiException {
    ApiClient apiClient = anApiClient(MANAGER1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    Comment createdComment = api.postComment(STUDENT1_ID, MANAGER_ID, createCommentByManager());

    assertEquals(commentCreatedByManager().getContent(), createdComment.getContent());
    assertEquals(
        commentCreatedByManager().getObserver().getRef(), createdComment.getObserver().getRef());
    assertEquals(
        commentCreatedByManager().getSubject().getRef(), createdComment.getSubject().getRef());
  }

  @Test
  void teacher_comment_about_student1_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TEACHER1_TOKEN);
    CommentsApi api = new CommentsApi(apiClient);

    Comment createdComment = api.postComment(STUDENT1_ID, TEACHER1_ID, createCommentByTeacher());

    assertEquals(commentCreatedByTeacher().getContent(), createdComment.getContent());
    assertEquals(
        commentCreatedByTeacher().getObserver().getRef(), createdComment.getObserver().getRef());
    assertEquals(
        commentCreatedByTeacher().getSubject().getRef(), createdComment.getSubject().getRef());
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, CommentIT.ContextInitializer.SERVER_PORT);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
