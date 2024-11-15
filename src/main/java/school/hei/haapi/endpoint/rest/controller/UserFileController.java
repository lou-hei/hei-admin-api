package school.hei.haapi.endpoint.rest.controller;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import school.hei.haapi.endpoint.rest.mapper.FileInfoMapper;
import school.hei.haapi.endpoint.rest.mapper.WorkDocumentMapper;
import school.hei.haapi.endpoint.rest.model.FileInfo;
import school.hei.haapi.endpoint.rest.model.FileType;
import school.hei.haapi.endpoint.rest.model.ProfessionalExperienceFileTypeEnum;
import school.hei.haapi.endpoint.rest.model.WorkDocumentInfo;
import school.hei.haapi.endpoint.rest.validator.CreateStudentWorkFileValidator;
import school.hei.haapi.model.BoundedPageSize;
import school.hei.haapi.model.PageFromOne;
import school.hei.haapi.service.StudentFileService;

@RestController
@AllArgsConstructor
public class UserFileController {
  private final StudentFileService fileService;
  private final FileInfoMapper fileInfoMapper;
  private final WorkDocumentMapper workDocumentMapper;
  private final CreateStudentWorkFileValidator createStudentWorkFileValidator;

  @GetMapping(
      value = "/students/{id}/scholarship_certificate/raw",
      produces = APPLICATION_PDF_VALUE)
  public byte[] getStudentScholarshipCertificate(@PathVariable(name = "id") String studentId) {
    return fileService.generatePdf(studentId, "scolarity");
  }

  @GetMapping(
      value = "/students/{student_id}/fees/{fee_id}/payments/{payment_id}/receipt/raw",
      produces = APPLICATION_PDF_VALUE)
  public byte[] getStudentReceiptFee(
      @PathVariable(name = "student_id") String studentId,
      @PathVariable(name = "fee_id") String feeId,
      @PathVariable(name = "payment_id") String paymentId) {
    return fileService.generatePaidFeeReceipt(feeId, paymentId, "paidFeeReceipt");
  }

  @PostMapping(value = "/users/{user_id}/files/raw", consumes = MULTIPART_FORM_DATA_VALUE)
  public FileInfo uploadStudentFile(
      @PathVariable(name = "user_id") String userId,
      @RequestParam(name = "filename") String fileName,
      @RequestParam(name = "file_type") FileType fileType,
      @RequestPart(name = "file_to_upload") MultipartFile fileToUpload) {
    return fileInfoMapper.toRest(
        fileService.uploadUserFile(fileName, fileType, userId, fileToUpload));
  }

  @PostMapping(
      value = "/students/{student_id}/work_files/raw",
      consumes = MULTIPART_FORM_DATA_VALUE)
  public WorkDocumentInfo uploadStudentWorkFile(
      @PathVariable(name = "student_id") String studentId,
      @RequestParam(name = "filename", required = true) String filename,
      @RequestParam(name = "commitment_begin", required = true) Instant commitmentBegin,
      @RequestParam(name = "commitment_end", required = false) Instant commitmentEnd,
      @RequestParam(name = "creation_datetime", required = false) Instant creationDatetime,
      @RequestParam(name = "experience_type", required = true)
          ProfessionalExperienceFileTypeEnum professionalExperience,
      @RequestPart(name = "file_to_upload") MultipartFile fileToUpload) {
    createStudentWorkFileValidator.acceptWorkDocumentField(
        filename, commitmentBegin, commitmentEnd, professionalExperience);
    return workDocumentMapper.toRest(
        fileService.uploadStudentWorkFile(
            studentId,
            filename,
            creationDatetime,
            commitmentBegin,
            commitmentEnd,
            fileToUpload,
            professionalExperience));
  }

  @GetMapping(value = "/students/{student_id}/work_files")
  public List<WorkDocumentInfo> getStudentWorkDocuments(
      @PathVariable(name = "student_id") String studentId,
      @RequestParam(name = "page") PageFromOne page,
      @RequestParam(name = "page_size") BoundedPageSize pageSize,
      @RequestParam(name = "professional_experience", required = false)
          ProfessionalExperienceFileTypeEnum professionalExperience) {
    return fileService
        .getStudentWorkFiles(studentId, professionalExperience, page, pageSize)
        .stream()
        .map(workDocumentMapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping(value = "/users/{user_id}/files")
  public List<FileInfo> getStudentFiles(
      @PathVariable(name = "user_id") String userId,
      @RequestParam(name = "file_type", required = false) FileType fileType,
      @RequestParam(name = "page") PageFromOne page,
      @RequestParam(name = "page_size") BoundedPageSize pageSize) {
    return fileService.getUserFiles(userId, fileType, page, pageSize).stream()
        .map(fileInfoMapper::toRest)
        .collect(toUnmodifiableList());
  }

  @GetMapping(value = "/students/{student_id}/work_files/{id}")
  public WorkDocumentInfo getStudentWorkDocumentsById(
      @PathVariable(name = "student_id") String studentId, @PathVariable(name = "id") String id) {
    return workDocumentMapper.toRest(fileService.getStudentWorkFileById(id));
  }

  @GetMapping(value = "/users/{user_id}/files/{id}")
  public FileInfo getStudentFilesById(
      @PathVariable(name = "user_id") String userId, @PathVariable(name = "id") String fileId) {
    return fileInfoMapper.toRest(fileService.getUserFileById(userId, fileId));
  }
}
