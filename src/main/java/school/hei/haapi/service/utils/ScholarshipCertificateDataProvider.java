package school.hei.haapi.service.utils;

import static school.hei.haapi.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
    String academicYear =
        getAcademicYear(findLastStudentPromotion(student).getStartDatetime(), Instant.now());
    return academicYear + " année d'informatique - parcours " + specializationFiledString(student);
  }

  public String getAcademicYear(Instant startDatetime, Instant from) {

    int firstYear = startDatetime.atZone(ZoneId.systemDefault()).getYear();

    LocalDate date = from.atZone(ZoneId.systemDefault()).toLocalDate();
    int year = date.getYear();
    int month = date.getMonthValue();

    int scholarYear = (month >= 11) ? year : year - 1;

    int difference = scholarYear - firstYear;

    return switch (difference) {
      case 0 -> "Première";
      case 1 -> "Deuxième";
      case 2 -> "Troisième";
      case 3 -> "Quatrième";
      case 4 -> "Cinquième";
      default -> "Non defini";
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

  public static String determinerAnneeEtudes(String nomPromotion, Instant startDateTime) {
    // Calcul de la différence en années entre maintenant et le début de la promotion
    Instant maintenant = Instant.now();
    long differenceAnnees =
        ChronoUnit.YEARS.between(
            startDateTime.atZone(ZoneId.systemDefault()).toLocalDate(),
            maintenant.atZone(ZoneId.systemDefault()).toLocalDate());

    return switch ((int) differenceAnnees) {
      case 0 -> "première année";
      case 1 -> "deuxième année";
      case 2 -> "troisième année";
      case 3 -> "quatrième année";
      case 4 -> "cinquième année";
      default -> "cinquième année";
    };
  }

  private Promotion findLastStudentPromotion(User student) {
    // TODO: getLast orderBy creationDatetime
    return promotionService.getAllStudentPromotions(student.getId()).getFirst();
  }
}
