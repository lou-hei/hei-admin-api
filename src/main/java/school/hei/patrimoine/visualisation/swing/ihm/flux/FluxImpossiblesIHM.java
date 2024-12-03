package school.hei.patrimoine.visualisation.swing.ihm.flux;

import java.util.Set;
import school.hei.patrimoine.modele.evolution.FluxJournalier;
import school.hei.patrimoine.visualisation.swing.modele.PatrimoinesVisualisables;

public final class FluxImpossiblesIHM extends FluxIHM {

  public FluxImpossiblesIHM(PatrimoinesVisualisables patrimoinesVisualisables) {
    super(patrimoinesVisualisables);
  }

  @Override
  protected int getIHMHeight() {
    return 300;
  }

  @Override
  protected String titre() {
    return "FLUX IMPOSSIBLES";
  }

  @Override
  protected Set<FluxJournalier> flux() {
    return patrimoinesVisualisables.getEvolutionPatrimoine().fluxJournaliersImpossibles();
  }
}
