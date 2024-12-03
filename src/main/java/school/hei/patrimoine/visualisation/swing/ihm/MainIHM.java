package school.hei.patrimoine.visualisation.swing.ihm;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.WEST;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.time.LocalDate.now;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BoxLayout.X_AXIS;
import static javax.swing.BoxLayout.Y_AXIS;
import static school.hei.patrimoine.modele.Devise.EUR;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.*;
import lombok.Getter;
import lombok.SneakyThrows;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.Personne;
import school.hei.patrimoine.visualisation.swing.ihm.flux.FluxImpossiblesIHM;
import school.hei.patrimoine.visualisation.swing.ihm.flux.FluxJournaliersIHM;
import school.hei.patrimoine.visualisation.swing.ihm.selecteur.SelecteurGrapheConfIHM;
import school.hei.patrimoine.visualisation.swing.ihm.selecteur.SelecteurPatrimoineIHM;
import school.hei.patrimoine.visualisation.swing.ihm.selecteur.SelecteurPeriodeIHM;
import school.hei.patrimoine.visualisation.swing.modele.GrapheConfObservable;
import school.hei.patrimoine.visualisation.swing.modele.PatrimoinesVisualisables;

public class MainIHM extends JFrame implements Observer {
  @Getter private final PatrimoinesVisualisables patrimoinesVisualisables;
  private final GrapheConfObservable grapheConfObservable = new GrapheConfObservable();

  private final SelecteurPatrimoineIHM selecteurPatrimoineIHM;
  private final SelecteurPeriodeIHM selecteurPeriodeIHM;

  private final FluxImpossiblesIHM fluxImpossiblesIHM;
  private final FluxJournaliersIHM fluxJournaliersIHM;

  private final EvolutionPatrimoineSelectionnéIHM evolutionPatrimoineSelectionnéIHM;
  private final SelecteurGrapheConfIHM selecteurGrapheConfIHM;

  public MainIHM(List<Patrimoine> patrimoines) {
    List<Patrimoine> patrimoinesAvecPersonnes = new ArrayList<>();
    patrimoinesAvecPersonnes.addAll(patrimoines);
    patrimoinesAvecPersonnes.addAll(patrimoinesPersonnels(patrimoines));
    this.patrimoinesVisualisables = new PatrimoinesVisualisables(patrimoinesAvecPersonnes);
    this.patrimoinesVisualisables.addObserver(this);

    this.selecteurPatrimoineIHM =
        new SelecteurPatrimoineIHM(patrimoinesVisualisables, grapheConfObservable);

    this.fluxImpossiblesIHM = new FluxImpossiblesIHM(patrimoinesVisualisables);
    this.fluxJournaliersIHM = new FluxJournaliersIHM(patrimoinesVisualisables);

    this.selecteurPeriodeIHM = new SelecteurPeriodeIHM(patrimoinesVisualisables);
    this.evolutionPatrimoineSelectionnéIHM =
        new EvolutionPatrimoineSelectionnéIHM(patrimoinesVisualisables, grapheConfObservable);
    this.selecteurGrapheConfIHM = new SelecteurGrapheConfIHM(grapheConfObservable);

    configureFrame();
    configureContentPane();
  }

  private static Set<Patrimoine> patrimoinesPersonnels(List<Patrimoine> patrimoines) {
    var personnes =
        patrimoines.stream()
            .flatMap(patrimoine -> patrimoine.getPossesseurs().keySet().stream())
            .collect(toSet());
    return personnes.stream().map(personne -> personne.patrimoine(EUR, now())).collect(toSet());
  }

  @SneakyThrows
  private void configureFrame() {
    setTitle();
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    pack();
    setSize(getDefaultToolkit().getScreenSize());
    setResizable(true);
    setVisible(true);
    setLocationRelativeTo(null);
  }

  private JPanel getFluxJournaliersIHM() {
    var fluxJournaliersBox = new BoxLayout(fluxJournaliersIHM, X_AXIS);
    fluxJournaliersIHM.setLayout(fluxJournaliersBox);
    fluxJournaliersIHM.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));

    var fixedWidth =
        new Dimension(
            fluxJournaliersIHM.getPreferredSize().width,
            fluxJournaliersIHM.getPreferredSize().height);
    fluxJournaliersIHM.setPreferredSize(fixedWidth);
    fluxJournaliersIHM.setMaximumSize(fixedWidth);
    fluxJournaliersIHM.setMinimumSize(fixedWidth);

    return fluxJournaliersIHM;
  }

  private void configureContentPane() {
    var contentPane = new JPanel();
    setContentPane(contentPane);
    contentPane.setLayout(new BorderLayout(0, 0));

    var westPanel = new JPanel();
    westPanel.setLayout(new BoxLayout(westPanel, Y_AXIS));
    westPanel.setAlignmentY(TOP_ALIGNMENT);
    var westMargin = 5;
    westPanel.setBorder(createEmptyBorder(westMargin, westMargin, westMargin, westMargin));

    westPanel.add(selecteurPatrimoineIHM);
    westPanel.add(selecteurGrapheConfIHM);
    westPanel.add(selecteurPeriodeIHM);
    westPanel.add(fluxImpossiblesIHM);

    westPanel.add(getFluxJournaliersIHM());
    contentPane.add(westPanel, WEST);
    contentPane.add(evolutionPatrimoineSelectionnéIHM, CENTER);
  }

  @Override
  public void update(Observable o, Object arg) {
    setTitle();
  }

  private void setTitle() {
    var p = patrimoinesVisualisables.getEvolutionPatrimoine().getPatrimoine();
    setTitle(
        String.format(
            "Patrimoine : possesseur=%s, t=%s",
            p.getPossesseurs().keySet().stream().map(Personne::nom).collect(joining(",")),
            p.getT()));
  }
}
