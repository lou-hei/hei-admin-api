package school.hei.patrimoine.visualisation.swing.ihm;

import java.util.Observable;
import java.util.Observer;
import school.hei.patrimoine.visualisation.swing.modele.GrapheConfObservable;
import school.hei.patrimoine.visualisation.swing.modele.PatrimoinesVisualisables;

public class EvolutionPatrimoineSelectionnéIHM extends EvolutionPatrimoineIHM implements Observer {

  public EvolutionPatrimoineSelectionnéIHM(
      PatrimoinesVisualisables patrimoinesVisualisables,
      GrapheConfObservable grapheConfObservable) {
    super(patrimoinesVisualisables::getEvolutionPatrimoine, grapheConfObservable::getGrapheConf);

    patrimoinesVisualisables.addObserver(this);

    grapheConfObservable.addObserver(this);
  }

  @Override
  public void update(Observable o, Object arg) {
    this.repaint();
  }
}
