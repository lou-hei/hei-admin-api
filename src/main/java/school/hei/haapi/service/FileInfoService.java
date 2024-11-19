package school.hei.haapi.service;

import static java.io.File.pathSeparator;
import static school.hei.haapi.service.aws.FileService.getFormattedBucketKey;

import java.io.File;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.rest.model.FileType;
import school.hei.haapi.endpoint.rest.model.ProfessionalExperienceFileTypeEnum;
import school.hei.haapi.model.FileInfo;
import school.hei.haapi.model.User;
import school.hei.haapi.model.WorkDocument;
import school.hei.haapi.model.validator.FilenameValidator;
import school.hei.haapi.repository.FileInfoRepository;
import school.hei.haapi.repository.WorkDocumentRepository;
import school.hei.haapi.service.aws.FileService;

@Service
@AllArgsConstructor
public class FileInfoService {
  private final WorkDocumentRepository workFileRepository;
  private final FileInfoRepository fileInfoRepository;
  private final UserService userService;
  private final FileService fileService;
  private final MultipartFileConverter multipartFileConverter;
  private final FilenameValidator filenameValidator;

  public WorkDocument uploadFile(
      User student,
      String filename,
      Instant creationDatetime,
      Instant commitmentBegin,
      Instant commitmentEnd,
      MultipartFile workFile,
      ProfessionalExperienceFileTypeEnum professionalExperience) {
    filenameValidator.accept(filename);
    // STUDENT/REF/WORK_DOCUMENT/filename
    String filePath =
        getFormattedBucketKey(student, "WORK_DOCUMENT", filename)
            + fileService.getFileExtension(workFile);

    WorkDocument workDocumentToSave =
        WorkDocument.builder()
            .commitmentBegin(commitmentBegin)
            .commitmentEnd(commitmentEnd)
            .creationDatetime(creationDatetime)
            .filePath(filePath)
            .filename(filename)
            .student(student)
            .professionalExperienceType(professionalExperience)
            .build();
    File file = multipartFileConverter.apply(workFile);
    fileService.uploadObjectToS3Bucket(filePath, file);

    return workFileRepository.save(workDocumentToSave);
  }

  public FileInfo uploadFile(
      String fileName, FileType fileType, String userId, MultipartFile fileToUpload) {
    filenameValidator.accept(fileName);
    User student = userService.findById(userId);
    // STUDENT/STUDENT_ref/<TRANSCRIPT|DOCUMENT|OTHER>/fileName.extension
    String filePath =
        getFormattedBucketKey(student, fileType, fileName)
            + fileService.getFileExtension(fileToUpload);
    FileInfo fileInfo =
        FileInfo.builder()
            .fileType(fileType)
            .name(fileName)
            .user(student)
            .filePath(filePath)
            .creationDatetime(Instant.now())
            .build();
    File file = multipartFileConverter.apply(fileToUpload);
    fileService.uploadObjectToS3Bucket(filePath, file);
    return fileInfoRepository.save(fileInfo);
  }

  public FileInfo uploadSchoolFile(String fileName, FileType fileType, MultipartFile fileToUpload) {
    // User would be null because School Files is not attached to a specific user.
    filenameValidator.accept(fileName);
    String endPath =
        "SCHOOL_FILE" + pathSeparator + fileName + fileService.getFileExtension(fileToUpload);
    File file = multipartFileConverter.apply(fileToUpload);
    fileService.uploadObjectToS3Bucket(endPath, file);
    FileInfo fileInfo =
        FileInfo.builder()
            .fileType(fileType)
            .name(fileName)
            .filePath(endPath)
            .creationDatetime(Instant.now())
            .build();
    return fileInfoRepository.save(fileInfo);
  }
}
