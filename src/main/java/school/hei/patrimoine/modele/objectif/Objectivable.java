package school.hei.patrimoine.modele.objectif;

import static java.util.stream.Collectors.toSet;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import school.hei.patrimoine.modele.Argent;

public abstract class Objectivable {
  private final Set<Objectif> objectifs = new HashSet<>();

  public abstract String nom();

  public abstract Argent valeurAObjectifT(LocalDate t);

  public final void addObjectif(Objectif objectif) {
    objectifs.add(objectif);
  }

  public final Set<ObjectifNonAtteint> verifier() {
    return objectifs.stream().flatMap(o -> o.verifier().stream()).collect(toSet());
  }
}
