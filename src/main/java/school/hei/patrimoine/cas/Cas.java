package school.hei.patrimoine.cas;

import static java.util.stream.Collectors.toSet;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.modele.Devise;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.Personne;
import school.hei.patrimoine.modele.objectif.ObjectifNonAtteint;
import school.hei.patrimoine.modele.possession.Compte;
import school.hei.patrimoine.modele.possession.Possession;

@Slf4j
public abstract class Cas {
  @Getter protected final LocalDate ajd;
  @Getter protected final LocalDate finSimulation;

  // lazy init required as spec is declarative, not procedural
  private final Supplier<Patrimoine> newPatrimoineSupplier;
  protected Patrimoine patrimoine;

  protected Cas(LocalDate ajd, LocalDate finSimulation, Map<Personne, Double> possesseurs) {
    this.ajd = ajd;
    this.finSimulation = finSimulation;
    this.newPatrimoineSupplier =
        () -> {
          patrimoine = Patrimoine.of(nom(), devise(), ajd, possesseurs, possessions());
          init();
          suivi();
          return patrimoine;
        };
  }

  protected Cas(LocalDate ajd, LocalDate finSimulation, Personne possesseur) {
    this(ajd, finSimulation, Map.of(possesseur, 1.));
  }

  public Patrimoine patrimoine() {
    return patrimoine == null ? newPatrimoineSupplier.get() : patrimoine;
  }

  protected abstract Devise devise();

  protected abstract String nom();

  protected abstract void init();

  protected abstract void suivi();

  public Set<ObjectifNonAtteint> verifier() {
    warnPrecociousFluxArgent();

    var nonAtteintsPatrimoines = patrimoine().verifier();
    var nonAtteintsPossessions =
        possessions().stream().flatMap(p -> p.verifier().stream()).collect(toSet());
    return Stream.concat(nonAtteintsPatrimoines.stream(), nonAtteintsPossessions.stream())
        .collect(toSet());
  }

  private void warnPrecociousFluxArgent() {
    patrimoine().getPossessions().stream()
        .filter(p -> p instanceof Compte)
        .flatMap(c -> ((Compte) c).getFluxArgents().stream())
        .forEach(
            p -> {
              if (p.getDebut().isBefore(ajd)) {
                log.warn("fluxArgent.debut before ajd found: {}", p);
              }
            });
  }

  public abstract Set<Possession> possessions();
}
