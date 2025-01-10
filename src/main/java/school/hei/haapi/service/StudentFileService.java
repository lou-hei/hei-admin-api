package school.hei.haapi.service;

import static java.time.LocalDate.now;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static school.hei.haapi.service.utils.DataFormatterUtils.formatLocalDate;
import static school.hei.haapi.service.utils.DataFormatterUtils.numberToReadable;
import static school.hei.haapi.service.utils.DataFormatterUtils.numberToWords;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import school.hei.haapi.datastructure.ListGrouper;
import school.hei.haapi.endpoint.event.EventProducer;
import school.hei.haapi.endpoint.event.model.SendReceiptZipToEmail;
import school.hei.haapi.endpoint.rest.model.FileType;
import school.hei.haapi.endpoint.rest.model.ProfessionalExperienceFileTypeEnum;
import school.hei.haapi.endpoint.rest.model.ZipReceiptsRequest;
import school.hei.haapi.endpoint.rest.model.ZipReceiptsStatistic;
import school.hei.haapi.model.*;
import school.hei.haapi.repository.FileInfoRepository;
import school.hei.haapi.repository.dao.FileInfoDao;
import school.hei.haapi.service.utils.Base64Converter;
import school.hei.haapi.service.utils.ClassPathResourceResolver;
import school.hei.haapi.service.utils.HtmlParser;
import school.hei.haapi.service.utils.PaidFeeReceiptDataProvider;
import school.hei.haapi.service.utils.PdfRenderer;
import school.hei.haapi.service.utils.ScholarshipCertificateDataProvider;

@Service
@AllArgsConstructor
public class StudentFileService {
  private final int MAX_RECEIPT_PDF_IN_ZIP_FILE = 3_600; // if pdf=27.3Ko, approximately 98,280 Mo

  private final Base64Converter base64Converter;
  private final ClassPathResourceResolver classPathResourceResolver;
  private final HtmlParser htmlParser;
  private final PdfRenderer pdfRenderer;
  private final UserService userService;
  private final PaymentService paymentService;
  private final FeeService feeService;
  private final ScholarshipCertificateDataProvider certificateDataProvider;
  private final FileInfoRepository fileInfoRepository;
  private final FileInfoService fileInfoService;
  private final WorkDocumentService workDocumentService;
  private final FileInfoDao fileInfoDao;
  private final ListGrouper<File> dataFileGrouper;
  private final EventProducer eventProducer;

  public WorkDocument uploadStudentWorkFile(
      String studentId,
      String filename,
      Instant creationDatetime,
      Instant commitmentBegin,
      Instant commitmentEnd,
      MultipartFile workFile,
      ProfessionalExperienceFileTypeEnum professionalExperience) {
    return workDocumentService.uploadStudentWorkFile(
        studentId,
        filename,
        creationDatetime,
        commitmentBegin,
        commitmentEnd,
        workFile,
        professionalExperience);
  }

  public List<WorkDocument> getStudentWorkFiles(
      String studentId,
      ProfessionalExperienceFileTypeEnum professionalExperience,
      PageFromOne page,
      BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(DESC, "creationDatetime"));
    return workDocumentService.getStudentWorkFiles(studentId, professionalExperience, pageable);
  }

  public WorkDocument getStudentWorkFileById(String workFileId) {
    return workDocumentService.getStudentWorkFileById(workFileId);
  }

  public FileInfo uploadUserFile(
      String fileName, FileType fileType, String userId, MultipartFile fileToUpload) {
    return fileInfoService.uploadFile(fileName, fileType, userId, fileToUpload);
  }

  public List<FileInfo> getUserFiles(
      String userId, FileType fileType, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable =
        PageRequest.of(page.getValue() - 1, pageSize.getValue(), Sort.by(DESC, "creationDatetime"));
    return fileInfoDao.findAllByCriteria(userId, fileType, pageable);
  }

  public FileInfo getUserFileById(String userId, String id) {
    return fileInfoRepository.getByUserIdAndId(userId, id);
  }

  public byte[] generatePdf(String studentId, String template) {
    Context context = loadContext(studentId);
    String html = htmlParser.apply(template, context);
    return pdfRenderer.apply(html);
  }

  public byte[] generatePaidFeeReceipt(String feeId, String paymenId, String template) {
    Fee fee = feeService.getById(feeId);
    Payment payment = paymentService.getById(paymenId);
    Context context = loadPaymentReceiptContext(fee, payment);
    String html = htmlParser.apply(template, context);
    return pdfRenderer.apply(html);
  }

  public ZipReceiptsStatistic getZipFeeReceipts(ZipReceiptsRequest zipReceiptsRequest) {
    List<Payment> allPayementBetween =
        paymentService.getAllPayementBetween(
            zipReceiptsRequest.getFrom(), zipReceiptsRequest.getTo());
    List<File> pdfs =
        allPayementBetween.stream()
            .map(
                (payment) -> {
                  byte[] paidFeeReceiptsData =
                      generatePaidFeeReceipt(
                          payment.getFee().getId(), payment.getId(), "paidFeeReceipt");
                  File file = null;
                  try {
                    file = File.createTempFile(UUID.randomUUID().toString(), ".pdf");
                    FileUtils.writeByteArrayToFile(file, paidFeeReceiptsData);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                  return file;
                })
            .toList();

    sendReceiptZipToEmail(pdfs, zipReceiptsRequest.getDestinationEmail());

    return new ZipReceiptsStatistic().fileCountInZip(pdfs.size());
  }

  public void sendReceiptZipToEmail(List<File> pdfs, String destinationEmail) {
    List<List<File>> groups = dataFileGrouper.apply(pdfs, MAX_RECEIPT_PDF_IN_ZIP_FILE);
    for (int groupId = 0; groupId < groups.size(); groupId++) {
      eventProducer.accept(
          Collections.singleton(
              SendReceiptZipToEmail.builder()
                  .startRequest(Instant.now())
                  .idWork(groupId)
                  .fileToZip(groups.get(groupId))
                  .emailRecipient(destinationEmail)
                  .build()));
    }
  }

  private Context loadPaymentReceiptContext(Fee fee, Payment payment) {
    Resource logo = classPathResourceResolver.apply("HEI_logo", ".png");
    Context context = new Context();
    List<Payment> paidPaymentsBefore =
        paymentService.getByFeeIdOrderByCreationDatetimeAsc(fee.getId());
    PaidFeeReceiptDataProvider dataProvider =
        new PaidFeeReceiptDataProvider(fee.getStudent(), fee, payment, paidPaymentsBefore);

    context.setVariable("logo", base64Converter.apply(logo));
    context.setVariable("paymentAuthorName", dataProvider.getEntirePaymentAuthorName());
    context.setVariable("receiptNumber", payment.getId());
    context.setVariable("totalAmount", numberToReadable(dataProvider.getFeeTotalAmount()));
    context.setVariable("paymentDate", dataProvider.getPaymentDate());
    context.setVariable("paymentAmount", numberToReadable(dataProvider.getTotalPaymentAmount()));
    context.setVariable("remainingAmount", numberToReadable(dataProvider.getRemainingAmount()));
    context.setVariable(
        "paymentAmountAsWords", numberToWords(dataProvider.getTotalPaymentAmount()));
    context.setVariable("paymentReason", dataProvider.getFeeComment());
    context.setVariable("paymentType", paymentType(dataProvider.getPaymentType()));

    return context;
  }

  private String paymentType(school.hei.haapi.endpoint.rest.model.Payment.TypeEnum typeEnum) {
    return switch (typeEnum) {
      case BANK_TRANSFER -> "Virement bancaire";
      case CASH -> "En espèce";
      case MOBILE_MONEY -> "Mobile Money";
      case SCHOLARSHIP, FIX -> null;
    };
  }

  private Context loadContext(String studentId) {
    Resource logo = classPathResourceResolver.apply("HEI_logo", ".png");
    Resource signature = classPathResourceResolver.apply("signature", ".png");
    User student = userService.findById(studentId);
    Context context = new Context();

    context.setVariable("student", student);
    context.setVariable("now", formatLocalDate(now()));
    context.setVariable(
        "academic_sentence", certificateDataProvider.getAcademicYearSentence(student));
    context.setVariable(
        "academic_promotion", certificateDataProvider.getAcademicYearPromotion(student));
    context.setVariable("birthday", formatLocalDate(student.getBirthDate(), "dd/MM/yyyy"));
    context.setVariable("logo", base64Converter.apply(logo));
    context.setVariable("signature", base64Converter.apply(signature));

    return context;
  }
}
