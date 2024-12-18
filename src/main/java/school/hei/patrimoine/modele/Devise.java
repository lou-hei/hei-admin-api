package school.hei.patrimoine.modele;

import static java.lang.Math.max;
import static java.time.Month.JULY;
import static java.time.temporal.ChronoUnit.DAYS;

import java.io.Serializable;
import java.time.LocalDate;

public record Devise(
    String nom, String symbole, LocalDate t, double valeurEnAriary, double tauxDappréciationAnnuel)
    implements Serializable {
  public static final Devise MGA = new Devise("ARIARY", "Ar", LocalDate.MIN, 1, 0.0);
  public static final Devise EUR =
      new Devise("EURO", "€", LocalDate.of(2024, JULY, 3), 4_821, 0.03);
  public static final Devise CAD =
      new Devise("CAD", "CAD", LocalDate.of(2024, JULY, 8), 3_286, 0.03);

  public double valeurEnAriary(LocalDate now) {
    var joursEcoules = DAYS.between(t, now);
    double valeurAjouteeJournaliere = valeurEnAriary * (tauxDappréciationAnnuel / 365.);
    return max(0, (int) (valeurEnAriary + valeurAjouteeJournaliere * joursEcoules));
  }
}
