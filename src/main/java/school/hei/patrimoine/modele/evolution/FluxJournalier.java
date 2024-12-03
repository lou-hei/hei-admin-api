package school.hei.patrimoine.modele.evolution;

import static java.util.stream.Collectors.joining;
import static school.hei.patrimoine.modele.evolution.SerieComptableTemporelle.parseMontant;

import java.time.LocalDate;
import java.util.Set;
import school.hei.patrimoine.modele.possession.Compte;
import school.hei.patrimoine.modele.possession.FluxArgent;

public record FluxJournalier(LocalDate date, Compte compte, Set<FluxArgent> flux) {
  @Override
  public String toString() {
    return String.format(
        "[%s][%s=%d%s] %s",
        date,
        compte.nom(),
        parseMontant(compte.valeurComptable()),
        compte.valeurComptable().devise().symbole(),
        toFluxJournalierString(flux));
  }

  private String toFluxJournalierString(Set<FluxArgent> flux) {
    return flux.stream().map(this::toFluxJournalierString).collect(joining(" "));
  }

  private String toFluxJournalierString(FluxArgent flux) {
    return String.format("(%s, %d)", flux.nom(), parseMontant((flux.getFluxMensuel())));
  }
}
