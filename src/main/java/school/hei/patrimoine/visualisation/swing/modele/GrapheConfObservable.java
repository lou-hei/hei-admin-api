package school.hei.patrimoine.visualisation.swing.modele;

import lombok.Getter;
import school.hei.patrimoine.visualisation.xchart.GrapheConf;

@Getter
public final class GrapheConfObservable extends ChangingObservable {
  private GrapheConf grapheConf = new GrapheConf(false, true, true, true, true);

  public GrapheConfObservable() {
    super();
  }

  public void setGrapheConf(GrapheConf grapheConf) {
    this.grapheConf = grapheConf;
    change();
  }
}
