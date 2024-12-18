package school.hei.patrimoine.modele.objectif;

import java.time.LocalDate;
import java.util.Optional;
import school.hei.patrimoine.modele.Argent;

public record Objectif(Objectivable objectivable, LocalDate t, Argent valeurComptable) {

  public Objectif(Objectivable objectivable, LocalDate t, Argent valeurComptable) {
    this.objectivable = objectivable;
    this.t = t;
    this.valeurComptable = valeurComptable;

    objectivable.addObjectif(this);
  }

  public Optional<ObjectifNonAtteint> verifier() {
    if (!objectivable.valeurAObjectifT(t).hasSameValeurComptable(valeurComptable, t)) {
      return Optional.of(new ObjectifNonAtteint(objectivable, this));
    }

    return Optional.empty();
  }
}
