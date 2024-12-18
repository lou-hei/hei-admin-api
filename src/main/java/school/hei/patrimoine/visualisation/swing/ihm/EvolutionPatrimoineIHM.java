package school.hei.patrimoine.visualisation.swing.ihm;

import java.awt.*;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import lombok.SneakyThrows;
import school.hei.patrimoine.modele.evolution.EvolutionPatrimoine;
import school.hei.patrimoine.visualisation.xchart.GrapheConf;
import school.hei.patrimoine.visualisation.xchart.GrapheurEvolutionPatrimoine;

public class EvolutionPatrimoineIHM extends JPanel {
  private final GrapheurEvolutionPatrimoine grapheurEvolutionPatrimoine;
  private final Supplier<EvolutionPatrimoine> evolutionPatrimoineGetter;
  private final Supplier<GrapheConf> grapheConfGetter;

  public EvolutionPatrimoineIHM(
      Supplier<EvolutionPatrimoine> evolutionPatrimoine, Supplier<GrapheConf> grapheConf) {
    super();
    this.grapheurEvolutionPatrimoine = new GrapheurEvolutionPatrimoine();
    this.evolutionPatrimoineGetter = evolutionPatrimoine;
    this.grapheConfGetter = grapheConf;
  }

  @SneakyThrows
  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    var grapheFile =
        grapheurEvolutionPatrimoine.apply(
            evolutionPatrimoineGetter.get(),
            grapheConfGetter.get().toBuilder().avecTitre(true).build());
    var grapheImage = ImageIO.read(grapheFile);
    g.drawImage(grapheImage, 0, 0, this.getWidth(), this.getHeight(), this);
  }
}
