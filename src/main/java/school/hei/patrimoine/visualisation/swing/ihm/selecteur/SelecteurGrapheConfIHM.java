package school.hei.patrimoine.visualisation.swing.ihm.selecteur;

import static java.awt.FlowLayout.LEFT;

import java.awt.*;
import java.util.Observable;
import java.util.Observer;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.swing.*;
import school.hei.patrimoine.visualisation.swing.ihm.FixedSizer;
import school.hei.patrimoine.visualisation.swing.modele.GrapheConfObservable;
import school.hei.patrimoine.visualisation.xchart.GrapheConf;

public class SelecteurGrapheConfIHM extends JPanel implements Observer {

  private final GrapheConfObservable grapheConfObservable;

  public SelecteurGrapheConfIHM(GrapheConfObservable grapheConfObservable) {
    super(new FlowLayout(LEFT));
    this.grapheConfObservable = grapheConfObservable;

    grapheConfObservable.addObserver(this);
    new FixedSizer().accept(this, new Dimension(500, 35));

    configureCheckBox(
        "Agrégat",
        GrapheConf::avecAgregat,
        (GrapheConf currentGrapheConf, Boolean isSelected) ->
            new GrapheConf(
                false,
                isSelected,
                currentGrapheConf.avecTresorerie(),
                currentGrapheConf.avecImmobilisations(),
                currentGrapheConf.avecObligations()));
    configureCheckBox(
        "Trésorerie",
        GrapheConf::avecTresorerie,
        (GrapheConf currentGrapheConf, Boolean isSelected) ->
            new GrapheConf(
                false,
                currentGrapheConf.avecAgregat(),
                isSelected,
                currentGrapheConf.avecImmobilisations(),
                currentGrapheConf.avecObligations()));
    configureCheckBox(
        "Immobilisations",
        GrapheConf::avecImmobilisations,
        (GrapheConf currentGrapheConf, Boolean isSelected) ->
            new GrapheConf(
                false,
                currentGrapheConf.avecAgregat(),
                currentGrapheConf.avecTresorerie(),
                isSelected,
                currentGrapheConf.avecObligations()));
    configureCheckBox(
        "Obligations",
        GrapheConf::avecObligations,
        (GrapheConf currentGrapheConf, Boolean isSelected) ->
            new GrapheConf(
                false,
                currentGrapheConf.avecAgregat(),
                currentGrapheConf.avecTresorerie(),
                currentGrapheConf.avecImmobilisations(),
                isSelected));
  }

  private void configureCheckBox(
      String text,
      Function<GrapheConf, Boolean> checkBoxValueSupplier,
      BiFunction<GrapheConf, Boolean, GrapheConf> newGrapheConfComputer) {
    var checkAgregat =
        new JCheckBox(text, checkBoxValueSupplier.apply(grapheConfObservable.getGrapheConf()));
    checkAgregat.addActionListener(
        e -> {
          var currentGrapheConf =
              grapheConfObservable.getGrapheConf(); // note(fresh-evolution-in-lambda)
          boolean isSelected = ((JCheckBox) e.getSource()).getModel().isSelected();
          grapheConfObservable.setGrapheConf(
              newGrapheConfComputer.apply(currentGrapheConf, isSelected));
        });

    add(checkAgregat);
  }

  @Override
  public void update(Observable o, Object arg) {
    this.repaint();
  }
}
