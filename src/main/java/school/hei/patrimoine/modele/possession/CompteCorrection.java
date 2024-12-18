package school.hei.patrimoine.modele.possession;

import static school.hei.patrimoine.modele.possession.TypeAgregat.CORRECTION;

import java.time.LocalDate;
import lombok.Getter;
import school.hei.patrimoine.modele.Argent;
import school.hei.patrimoine.modele.Devise;

@Getter
public final class CompteCorrection extends Possession {

  final Compte compte;

  public CompteCorrection(String nom, Devise devise) {
    this(
        "Correction[" + nom + "]",
        new Compte(
            String.format("Correction.Argent[%s]", nom), LocalDate.MIN, new Argent(0, devise)));
  }

  private CompteCorrection(String nom, Compte compte) {
    super(nom, compte.t, compte.valeurComptable);
    this.compte = compte;
  }

  @Override
  public CompteCorrection projectionFuture(LocalDate tFutur) {
    return new CompteCorrection(nom, compte.projectionFuture(tFutur));
  }

  @Override
  public TypeAgregat typeAgregat() {
    return CORRECTION;
  }
}
