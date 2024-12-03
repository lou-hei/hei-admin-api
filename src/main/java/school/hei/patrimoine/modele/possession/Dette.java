package school.hei.patrimoine.modele.possession;

import static school.hei.patrimoine.modele.possession.TypeAgregat.OBLIGATION;

import java.time.LocalDate;
import school.hei.patrimoine.modele.Argent;

public final class Dette extends Compte {

  public Dette(String nom, LocalDate t, Argent valeurComptable) {
    super(nom, t, valeurComptable);
    if (valeurComptable.gt(0)) {
      throw new IllegalArgumentException();
    }
  }

  private Dette(Compte compte) {
    this(compte.nom, compte.t, compte.valeurComptable);
  }

  @Override
  public Dette projectionFuture(LocalDate tFutur) {
    return new Dette(super.projectionFuture(tFutur));
  }

  @Override
  public TypeAgregat typeAgregat() {
    return OBLIGATION;
  }
}
