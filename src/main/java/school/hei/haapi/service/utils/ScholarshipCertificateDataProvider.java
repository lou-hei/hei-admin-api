package school.hei.haapi.service.utils;

import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.model.SpecializationField;
import school.hei.haapi.model.Promotion;
import school.hei.haapi.model.User;
import school.hei.haapi.model.exception.ApiException;
import school.hei.haapi.service.PromotionService;

@Component
@AllArgsConstructor
public class ScholarshipCertificateDataProvider {
  private final SchoolYearGetter schoolYearGetter;
  private final PromotionService promotionService;

  public String getAcademicYearSentence(User student) {
    String academicYear = getAcademicYear(student, Instant.now());
    return academicYear + " année d'informatique - parcours " + specializationFiledString(student);
  }

  public String getAcademicYear(User student, Instant from) {
    ZonedDateTime lastPromotionStartDatetime =
        ZonedDateTime.ofInstant(
            findLastStudentPromotion(student).getStartDatetime(), ZoneId.of("UTC+3"));
    ZonedDateTime zonedDateTimeNow = from.atZone(ZoneId.of("UTC+3"));
    int currentYear = zonedDateTimeNow.getYear();
    int lastPromotionYear = lastPromotionStartDatetime.getYear();

    int year = currentYear - lastPromotionYear;

    return switch (year) {
      case 0 -> "Première";
      case 1 -> "Deuxième";
      case 2, 3 -> "Troisième";
      default -> throw new ApiException(SERVER_EXCEPTION, "Invalid year");
    };
  }

  public String getAcademicYearPromotion(User student) {
    return " année scolaire " + schoolYearGetter.getSchoolYear();
  }

  private String specializationFiledString(User student) {
    SpecializationField specializationField = student.getSpecializationField();
    return switch (specializationField) {
      case COMMON_CORE -> "Tronc commun";
      case TN -> "Transformation Numérique";
      case EL -> "Écosystème Logiciel";
      default -> throw new ApiException(SERVER_EXCEPTION, "Invalid specialization field");
    };
  }

  private Promotion findLastStudentPromotion(User student) {
    // TODO: getLast orderBy creationDatetime
    return promotionService.getAllStudentPromotions(student.getId()).getFirst();
  }
}
