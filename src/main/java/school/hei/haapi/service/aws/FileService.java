package school.hei.haapi.service.aws;

import static school.hei.haapi.model.User.Role.MANAGER;
import static school.hei.haapi.model.User.Role.STUDENT;
import static school.hei.haapi.model.User.Role.TEACHER;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.file.BucketComponent;
import school.hei.haapi.file.FileHash;
import school.hei.haapi.file.FileTyper;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.BadRequestException;

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
      default -> throw new BadRequestException("Unexpected type " + user.getRole());
    };
  }

  public File createTempFile(byte[] bytes) {
    File tempFile;
    try {
      tempFile = File.createTempFile("file", "temp");
      FileOutputStream outputStream = new FileOutputStream(tempFile);
      outputStream.write(bytes);
      return tempFile;
    } catch (IOException ioException) {
      throw new ApiException(SERVER_EXCEPTION, ioException.getMessage());
    }
  }

  public File createTempFile(MultipartFile file) {
    File tempFile;
    try {
      tempFile = File.createTempFile("file", "temp");
      FileOutputStream outputStream = new FileOutputStream(tempFile);
      outputStream.write(file.getBytes());
      return tempFile;
    } catch (IOException ioException) {
      throw new ApiException(SERVER_EXCEPTION, ioException.getMessage());
    }
  }
}
