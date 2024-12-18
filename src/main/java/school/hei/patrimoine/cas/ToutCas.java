package school.hei.patrimoine.cas;

import static java.util.stream.Collectors.toSet;
import static school.hei.patrimoine.modele.Devise.EUR;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import school.hei.patrimoine.modele.Devise;
import school.hei.patrimoine.modele.Personne;
import school.hei.patrimoine.modele.objectif.Objectif;
import school.hei.patrimoine.modele.possession.Possession;

public class ToutCas extends Cas {

  private final CasSet casSet;

  public ToutCas(LocalDate ajd, LocalDate finSimulation, CasSet casSet) {
    super(ajd, finSimulation, toutPossesseurs(casSet));
    this.casSet = casSet;
  }

  private static Map<Personne, Double> toutPossesseurs(CasSet casSet) {
    var toutPersonnes =
        casSet.set().stream()
            .flatMap(cas -> cas.patrimoine().getPossesseurs().keySet().stream())
            .collect(toSet());
    Map<Personne, Double> possesseurs = new HashMap<>();
    toutPersonnes.forEach(p -> possesseurs.put(p, Double.NaN)); // TODO: NaN?
    return possesseurs;
  }

  @Override
  protected String nom() {
    return "Tout";
  }

  @Override
  protected Devise devise() {
    return EUR;
  }

  @Override
  protected void init() {}

  @Override
  public Set<Possession> possessions() {
    return casSet.set().stream().flatMap(cas -> cas.possessions().stream()).collect(toSet());
  }

  @Override
  protected void suivi() {
    new Objectif(patrimoine, finSimulation, casSet.objectifFinal());
  }
}
