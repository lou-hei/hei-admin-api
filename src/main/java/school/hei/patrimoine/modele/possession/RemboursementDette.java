package school.hei.patrimoine.modele.possession;

import static school.hei.patrimoine.modele.Argent.euro;
import static school.hei.patrimoine.modele.possession.TypeAgregat.FLUX;

import java.time.LocalDate;
import java.util.Set;
import school.hei.patrimoine.modele.Argent;

public final class RemboursementDette extends Possession {
  private final GroupePossession commeGroupe;

  public RemboursementDette(
      String nom,
      Compte rembourseur,
      Compte remboursé,
      Dette dette,
      Creance creance,
      LocalDate t,
      Argent montant) {
    this(
        new GroupePossession(
            nom,
            remboursé.devise(),
            t,
            Set.of(
                new TransfertArgent(nom + " (transfert)", rembourseur, remboursé, t, montant),
                new FluxArgent(nom + " (annulation dette)", dette, t, montant),
                new FluxArgent(nom + " (annulation créance)", creance, t, montant.mult(-1)))));
  }

  private RemboursementDette(GroupePossession commeGroupe) {
    super(commeGroupe.nom, LocalDate.MIN, euro(0));
    this.commeGroupe = commeGroupe;
  }

  @Override
  public RemboursementDette projectionFuture(LocalDate tFutur) {
    return new RemboursementDette(commeGroupe.projectionFuture(tFutur));
  }

  @Override
  public TypeAgregat typeAgregat() {
    return FLUX;
  }
}
