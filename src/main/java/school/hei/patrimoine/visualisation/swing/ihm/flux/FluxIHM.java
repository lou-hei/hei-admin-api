package school.hei.patrimoine.visualisation.swing.ihm.flux;

import static java.awt.FlowLayout.LEFT;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import java.awt.*;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.*;
import school.hei.patrimoine.modele.evolution.FluxJournalier;
import school.hei.patrimoine.visualisation.swing.ihm.FixedSizer;
import school.hei.patrimoine.visualisation.swing.modele.PatrimoinesVisualisables;

public abstract sealed class FluxIHM extends JPanel implements Observer
    permits FluxImpossiblesIHM, FluxJournaliersIHM {
  protected final PatrimoinesVisualisables patrimoinesVisualisables;
  private final JTextPane fluxTextPane;

  public FluxIHM(PatrimoinesVisualisables patrimoinesVisualisables) {
    super(new FlowLayout(LEFT));
    this.patrimoinesVisualisables = patrimoinesVisualisables;
    this.patrimoinesVisualisables.addObserver(this);

    fluxTextPane = new JTextPane();
    fixPanelSize();
    updateTextPane();
    this.add(new JScrollPane(fluxTextPane));
  }

  private void fixPanelSize() {
    new FixedSizer().accept(fluxTextPane, new Dimension(500, getIHMHeight()));
  }

  protected abstract int getIHMHeight();

  private void updateTextPane() {
    var flux =
        flux().stream()
            .sorted(comparing(FluxJournalier::date))
            .map(FluxJournalier::toString)
            .collect(joining("\n\n"));
    fluxTextPane.setText("".equals(flux) ? "" : (String.format("!! %s !!\n\n", titre()) + flux));
  }

  protected abstract String titre();

  protected abstract Set<FluxJournalier> flux();

  @Override
  public void update(Observable o, Object arg) {
    updateTextPane();
  }
}
