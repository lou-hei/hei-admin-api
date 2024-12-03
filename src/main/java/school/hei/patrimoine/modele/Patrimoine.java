package school.hei.patrimoine.modele;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import school.hei.patrimoine.modele.objectif.Objectivable;
import school.hei.patrimoine.modele.possession.CompteCorrection;
import school.hei.patrimoine.modele.possession.Possession;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@Getter
public final class Patrimoine extends Objectivable
    implements Serializable /*note(no-serializable)*/ {

  private final String nom;
  private final Devise devise;
  private final LocalDate t;
  private final Map<Personne, Double> possesseurs;
  private final Set<Possession> possessions;

  public static Patrimoine of(
      String nom,
      Devise devise,
      LocalDate t,
      Map<Personne, Double> possesseurs,
      Set<Possession> possessions) {
    var patrimoine =
        new Patrimoine(nom, devise, t, possesseurs, withComptesCorrections(possessions));
    possesseurs.forEach((possesseur, _taux) -> possesseur.addPatrimoine(patrimoine));
    return patrimoine;
  }

  public static Patrimoine of(
      String nom, Devise devise, LocalDate t, Personne possesseur, Set<Possession> possessions) {
    return Patrimoine.of(nom, devise, t, Map.of(possesseur, 1.), possessions);
  }

  private static Set<Possession> withComptesCorrections(Set<Possession> possessions) {
    var set = new HashSet<Possession>();
    possessions.forEach(
        p -> {
          set.add(p);
          set.add(p.getCompteCorrection());
        });
    return set;
  }

  public Argent getValeurComptable() {
    return getValeurComptable(devise);
  }

  public Argent getValeurComptable(Devise autreDevise) {
    return possessions.stream()
        .filter(not(p -> p instanceof CompteCorrection))
        .map(p -> p.valeurComptable().convertir(autreDevise, t))
        .reduce(new Argent(0, autreDevise), (a1, a2) -> a1.add(a2, t));
  }

  public Patrimoine projectionFuture(LocalDate tFutur) {
    return new Patrimoine(
        nom,
        devise,
        tFutur,
        possesseurs,
        possessions.stream().map(p -> p.projectionFuture(tFutur)).collect(toSet()));
  }

  public Possession possessionParNom(String nom) {
    return possessions.stream().filter(p -> nom.equals(p.nom())).findFirst().orElseThrow();
  }

  @Override
  public String nom() {
    return nom;
  }

  @Override
  public Argent valeurAObjectifT(LocalDate t) {
    return projectionFuture(t).getValeurComptable();
  }
}
