package school.hei.haapi.service.aws;

import static jxl.biff.FormatRecord.logger;
import static school.hei.haapi.model.User.Role.ADMIN;
import static school.hei.haapi.model.User.Role.MANAGER;
import static school.hei.haapi.model.User.Role.ORGANIZER;
import static school.hei.haapi.model.User.Role.STAFF_MEMBER;
import static school.hei.haapi.model.User.Role.STUDENT;
import static school.hei.haapi.model.User.Role.TEACHER;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.rest.model.FileType;
import school.hei.haapi.file.bucket.BucketComponent;
import school.hei.haapi.file.hash.FileHash;
import school.hei.haapi.file.zip.FileTyper;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.BadRequestException;
import software.amazon.awssdk.utils.IoUtils;

@Component
@Service
@AllArgsConstructor
public class FileService {
  private final BucketComponent bucketComponent;
  private final FileTyper fileTyper;

  public String getPresignedUrl(String key, Long durationExpirationSeconds) {
    Instant now = Instant.now();
    Instant expirationInstant = now.plusSeconds(durationExpirationSeconds);
    Duration expirationDuration = Duration.between(now, expirationInstant);
    return bucketComponent.presign(key, expirationDuration).toString();
  }

  public FileHash uploadObjectToS3Bucket(String key, File file) {
    return bucketComponent.upload(file, key);
  }

  public String getFileExtension(MultipartFile file) {
    return "." + Objects.requireNonNull(file.getContentType()).split("/")[1];
  }

  public static String getFormattedBucketKey(User user, String fileType) {
    return switch (user.getRole()) {
      case MANAGER -> String.format("%s/%s/%s_%s", MANAGER, user.getRef(), fileType, user.getRef());
      case TEACHER -> String.format("%s/%s/%s_%s", TEACHER, user.getRef(), fileType, user.getRef());
      case STUDENT -> String.format("%s/%s/%s_%s", STUDENT, user.getRef(), fileType, user.getRef());
      case STAFF_MEMBER ->
          String.format("%s/%s/%s_%s", STAFF_MEMBER, user.getRef(), fileType, user.getRef());
      case ADMIN -> String.format("%s/%s/%s_%s", ADMIN, user.getRef(), fileType, user.getRef());
      case ORGANIZER ->
          String.format("%s/%s/%s_%s", ORGANIZER, user.getRef(), fileType, user.getRef());
      default -> throw new BadRequestException("Unexpected type " + user.getRole());
    };
  }

  public static String getFormattedBucketKey(User user, String fileType, String fileName) {
    return switch (user.getRole()) {
      case MANAGER -> String.format("%s/%s/%s/%s", MANAGER, user.getRef(), fileType, fileName);
      case TEACHER -> String.format("%s/%s/%s/%s", TEACHER, user.getRef(), fileType, fileName);
      case STUDENT -> String.format("%s/%s/%s/%s", STUDENT, user.getRef(), fileType, fileName);
      case STAFF_MEMBER ->
          String.format("%s/%s/%s/%s", STAFF_MEMBER, user.getRef(), fileType, fileName);
      case ADMIN -> String.format("%s/%s/%s/%s", ADMIN, user.getRef(), fileType, fileName);
      case ORGANIZER -> String.format("%s/%s/%s/%s", ORGANIZER, user.getRef(), fileType, fileName);
      default -> throw new BadRequestException("Unexpected type " + user.getRole());
    };
  }

  public static String getFormattedBucketKey(User user, FileType fileType, String fileName) {
    return switch (user.getRole()) {
      case MANAGER -> String.format("%s/%s/%s/%s", MANAGER, user.getRef(), fileType, fileName);
      case TEACHER -> String.format("%s/%s/%s/%s", TEACHER, user.getRef(), fileType, fileName);
      case STUDENT -> String.format("%s/%s/%s/%s", STUDENT, user.getRef(), fileType, fileName);
      case STAFF_MEMBER ->
          String.format("%s/%s/%s/%s", STAFF_MEMBER, user.getRef(), fileType, fileName);
      case ADMIN -> String.format("%s/%s/%s/%s", ADMIN, user.getRef(), fileType, fileName);
      case ORGANIZER -> String.format("%s/%s/%s/%s", ORGANIZER, user.getRef(), fileType, fileName);
      default -> throw new BadRequestException("Unexpected type " + user.getRole());
    };
  }

  /* Use the JDK HttpClient (since v11) class to do the download. */
  public byte[] useHttpClientToGet(String presignedUrlString) {
    ByteArrayOutputStream byteArrayOutputStream =
        new ByteArrayOutputStream(); // Capture the response body to a byte array.

    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
    HttpClient httpClient = HttpClient.newHttpClient();
    try {
      URL presignedUrl = new URL(presignedUrlString);
      HttpResponse<InputStream> response =
          httpClient.send(
              requestBuilder.uri(presignedUrl.toURI()).GET().build(),
              HttpResponse.BodyHandlers.ofInputStream());

      IoUtils.copy(response.body(), byteArrayOutputStream);

      logger.info("HTTP response code is " + response.statusCode());
    } catch (URISyntaxException | InterruptedException | IOException e) {
      logger.error(e.getMessage(), e);
    }
    return byteArrayOutputStream.toByteArray();
  }
}
