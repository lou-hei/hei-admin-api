package school.hei.patrimoine.visualisation.swing.ihm.flux;

import java.util.Set;
import school.hei.patrimoine.modele.evolution.FluxJournalier;
import school.hei.patrimoine.visualisation.swing.modele.PatrimoinesVisualisables;

public final class FluxJournaliersIHM extends FluxIHM {

  public FluxJournaliersIHM(PatrimoinesVisualisables patrimoinesVisualisables) {
    super(patrimoinesVisualisables);
  }

  @Override
  protected int getIHMHeight() {
    return 500;
  }

  @Override
  protected String titre() {
    return "FLUX JOURNALIERS";
  }

  @Override
  protected Set<FluxJournalier> flux() {
    return patrimoinesVisualisables.getEvolutionPatrimoine().getFluxJournaliers();
  }
}
