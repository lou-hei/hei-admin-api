package school.hei.patrimoine.visualisation.swing.ihm.flux;

import static java.awt.Toolkit.getDefaultToolkit;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.evolution.EvolutionPatrimoine;
import school.hei.patrimoine.visualisation.swing.ihm.EvolutionPatrimoineIHM;
import school.hei.patrimoine.visualisation.xchart.GrapheConf;

public class TousGrapheIHM extends JDialog {
  public TousGrapheIHM(
      List<Patrimoine> patrimoines, LocalDate debut, LocalDate fin, GrapheConf grapheConf) {
    super();
    if (patrimoines.size() > 9) {
      throw new IllegalArgumentException("Ne peut afficher que 9 patrimoines maximum");
    }

    pack();
    setSize(getDefaultToolkit().getScreenSize());
    setVisible(true);
    setResizable(false);
    setLocationRelativeTo(null);

    var contentPane = new JPanel();
    setContentPane(contentPane);
    contentPane.setLayout(new GridLayout(3, 3));
    patrimoines.forEach(
        patrimoine ->
            contentPane.add(
                new EvolutionPatrimoineIHM(
                    () -> new EvolutionPatrimoine(patrimoine.nom(), patrimoine, debut, fin),
                    () -> grapheConf)));
  }
}
