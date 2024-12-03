package school.hei.patrimoine.cas;

import static java.awt.EventQueue.invokeLater;
import static java.util.Comparator.comparing;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.objectif.ObjectifNonAtteint;
import school.hei.patrimoine.visualisation.swing.ihm.MainIHM;

public class CasSetAnalyzer implements Consumer<CasSet> {

  public static void main(String[] args) {
    new CasSetAnalyzer().accept(new CasSetSupplier().get());
  }

  @Override
  public void accept(CasSet casSet) {
    var aCas = casSet.set().stream().toList().getFirst();
    var patrimoineTout = new ToutCas(aCas.getAjd(), aCas.getFinSimulation(), casSet);
    verifie(patrimoineTout);
    visualise(casSet.set(), patrimoineTout);
  }

  private static void verifie(ToutCas patrimoineTout) {
    var objectifsNonAtteints = patrimoineTout.verifier();
    if (!objectifsNonAtteints.isEmpty()) {
      throw new RuntimeException(
          "Objectifs non atteints : "
              + objectifsNonAtteints.stream()
                  .map(ObjectifNonAtteint::prettyPrint)
                  .collect(Collectors.joining("\n")));
    }
  }

  private static void visualise(Set<Cas> casSet, ToutCas patrimoineTout) {
    invokeLater(
        () ->
            new MainIHM(
                Stream.concat(casSet.stream(), Set.of(patrimoineTout).stream())
                    .map(Cas::patrimoine)
                    .sorted(comparing(Patrimoine::nom))
                    .toList()));
  }
}
