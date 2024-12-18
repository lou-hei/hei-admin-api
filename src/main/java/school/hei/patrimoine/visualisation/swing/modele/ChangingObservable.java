package school.hei.patrimoine.visualisation.swing.modele;

import java.util.Observable;

public sealed class ChangingObservable extends Observable
    permits GrapheConfObservable, PatrimoinesVisualisables {

  protected void change() {
    setChanged();
    notifyObservers();
  }
}
