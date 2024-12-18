package school.hei.patrimoine.visualisation.swing.modele;

import static java.time.Month.DECEMBER;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.evolution.EvolutionPatrimoine;

public final class PatrimoinesVisualisables extends ChangingObservable {

  @Getter private final List<Patrimoine> patrimoines;
  private Patrimoine patrimoineSélectionné;
  private LocalDate débutEvolution;
  private LocalDate finEvolution;

  public PatrimoinesVisualisables(List<Patrimoine> patrimoines) {
    super();
    this.patrimoines = patrimoines;
    this.patrimoineSélectionné = patrimoines.get(0);
    this.débutEvolution = patrimoineSélectionné.getT();
    this.finEvolution = LocalDate.of(2025, DECEMBER, 31);
  }

  public List<String> noms() {
    return patrimoines.stream().map(Patrimoine::nom).toList();
  }

  public void setDébutEvolution(LocalDate date) {
    this.débutEvolution = date;
    change();
  }

  public void setFinEvolution(LocalDate date) {
    this.finEvolution = date;
    change();
  }

  public Patrimoine selectionne(String nom) {
    this.patrimoineSélectionné =
        patrimoines.stream().filter(patrimoine -> nom.equals(patrimoine.nom())).findFirst().get();

    change();
    return this.patrimoineSélectionné;
  }

  public EvolutionPatrimoine getEvolutionPatrimoine() {
    return new EvolutionPatrimoine(
        patrimoineSélectionné.nom(), patrimoineSélectionné, débutEvolution, finEvolution);
  }
}
