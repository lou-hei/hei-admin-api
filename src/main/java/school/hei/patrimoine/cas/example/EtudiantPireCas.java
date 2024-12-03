package school.hei.patrimoine.cas.example;

import static java.time.Month.FEBRUARY;
import static java.time.Month.MAY;
import static java.time.Month.SEPTEMBER;
import static school.hei.patrimoine.modele.Argent.ariary;
import static school.hei.patrimoine.modele.Devise.MGA;

import java.time.LocalDate;
import java.util.Set;
import school.hei.patrimoine.cas.Cas;
import school.hei.patrimoine.modele.Devise;
import school.hei.patrimoine.modele.Personne;
import school.hei.patrimoine.modele.possession.Compte;
import school.hei.patrimoine.modele.possession.Correction;
import school.hei.patrimoine.modele.possession.FluxArgent;
import school.hei.patrimoine.modele.possession.Materiel;
import school.hei.patrimoine.modele.possession.Possession;

public class EtudiantPireCas extends Cas {

  private final Compte financeur;

  public EtudiantPireCas() {
    super(LocalDate.of(2024, SEPTEMBER, 17), LocalDate.MAX, new Personne("Ilo"));
    financeur = new Compte("Espèces", LocalDate.MIN, ariary(0));
  }

  @Override
  protected String nom() {
    return "Ilo";
  }

  @Override
  protected Devise devise() {
    return MGA;
  }

  @Override
  protected void init() {
    new FluxArgent(
        "Init compte Espèces", financeur, LocalDate.of(2024, FEBRUARY, 1), ariary(700_000));
  }

  @Override
  public Set<Possession> possessions() {
    var au13mai24 = LocalDate.of(2024, MAY, 13);
    var trainDeVie =
        new FluxArgent(
            "Vie courante",
            financeur,
            au13mai24.minusDays(100),
            au13mai24.plusDays(100),
            15,
            ariary(-100_000));
    var mac = new Materiel("MacBook Pro", au13mai24.minusDays(3), au13mai24, ariary(500_000), -0.9);
    return Set.of(financeur, trainDeVie, mac);
  }

  @Override
  protected void suivi() {
    new Correction(new FluxArgent("Correction à la hausse", financeur, ajd, ariary(50_000)));
  }
}
