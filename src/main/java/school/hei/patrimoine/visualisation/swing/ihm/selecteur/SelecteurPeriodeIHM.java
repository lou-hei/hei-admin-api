package school.hei.patrimoine.visualisation.swing.ihm.selecteur;

import static java.awt.FlowLayout.LEFT;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import javax.swing.*;
import org.jdatepicker.DateModel;
import org.jdatepicker.JDatePicker;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import school.hei.patrimoine.visualisation.swing.ihm.FixedSizer;
import school.hei.patrimoine.visualisation.swing.ihm.selecteur.jdatepicker.DateFormatter;
import school.hei.patrimoine.visualisation.swing.modele.PatrimoinesVisualisables;

public class SelecteurPeriodeIHM extends JPanel implements Observer {
  private final PatrimoinesVisualisables patrimoinesVisualisables;

  public SelecteurPeriodeIHM(PatrimoinesVisualisables patrimoinesVisualisables) {
    super(new FlowLayout(LEFT));
    this.patrimoinesVisualisables = patrimoinesVisualisables;
    this.patrimoinesVisualisables.addObserver(this);

    configurePeriodeEvolution();
    new FixedSizer().accept(this, new Dimension(500, 35));
  }

  private void configurePeriodeEvolution() {
    var ancienneEvolution = patrimoinesVisualisables.getEvolutionPatrimoine();
    this.add(
        ((Component)
            datePicker(
                "De",
                ancienneEvolution.getDebut(),
                e ->
                    patrimoinesVisualisables.setDébutEvolution(
                        // note(fresh-evolution-in-lambda): MUST use function to have fresh copy
                        // inside lambda
                        toLocalDate(e)))));
    this.add(
        ((Component)
            datePicker(
                "À",
                ancienneEvolution.getFin(),
                e ->
                    patrimoinesVisualisables.setFinEvolution(
                        // note(fresh-evolution-in-lambda)
                        toLocalDate(e)))));
  }

  private LocalDate toLocalDate(ActionEvent e) {
    return toLocalDate(((JDatePanelImpl) e.getSource()).getModel());
  }

  private LocalDate toLocalDate(DateModel<?> model) {
    return LocalDate.of(model.getYear(), model.getMonth() + 1, model.getDay());
  }

  private JDatePicker datePicker(String label, LocalDate parDefaut, ActionListener actionListener) {
    this.add(new JLabel(label));

    var i18n = new Properties();
    i18n.put("text.today", "ce jour");
    var datePanel =
        new JDatePanelImpl(
            new UtilDateModel(
                Date.from(parDefaut.atStartOfDay(ZoneId.systemDefault()).toInstant())),
            i18n);
    var datePicker = new JDatePickerImpl(datePanel, new DateFormatter());
    datePicker.addActionListener(actionListener);
    this.add(datePicker);

    return datePicker;
  }

  @Override
  public void update(Observable o, Object arg) {
    this.repaint();
  }
}
