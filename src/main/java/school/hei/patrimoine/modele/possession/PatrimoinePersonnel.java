package school.hei.patrimoine.modele.possession;

import static school.hei.patrimoine.modele.possession.TypeAgregat.PATRIMOINE;

import java.time.LocalDate;
import school.hei.patrimoine.modele.Argent;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.Personne;

public final class PatrimoinePersonnel extends Possession {

  private final Patrimoine patrimoine;
  private final Personne personne;

  public PatrimoinePersonnel(Patrimoine patrimoine, Personne personne) {
    super(
        String.format("Patrimoine %s de %s", patrimoine.nom(), personne.nom()),
        patrimoine.getT(),
        patrimoine.getValeurComptable());
    this.patrimoine = patrimoine;
    this.personne = personne;
  }

  @Override
  public Argent valeurComptable() {
    var valeurComptablePourToutPossesseurs = super.valeurComptable();
    var tauxPersonne = patrimoine.getPossesseurs().get(personne);
    return valeurComptablePourToutPossesseurs.mult(tauxPersonne);
  }

  @Override
  public Possession projectionFuture(LocalDate tFutur) {
    return new PatrimoinePersonnel(patrimoine.projectionFuture(tFutur), personne);
  }

  @Override
  public TypeAgregat typeAgregat() {
    return PATRIMOINE;
  }
}
