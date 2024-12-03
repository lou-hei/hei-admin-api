package school.hei.patrimoine.modele.possession;

import static school.hei.patrimoine.modele.possession.TypeAgregat.CORRECTION;

import java.time.LocalDate;

public final class Correction extends Possession {

  private final Possession possession;
  private final CompteCorrection compteCorrection;

  public Correction(FluxArgent fluxArgent) {
    super(
        String.format("Correction[p=%s,%s]", fluxArgent.getCompte().nom, fluxArgent.nom),
        fluxArgent.t,
        fluxArgent.valeurComptable);
    this.possession = fluxArgent.getCompte();
    this.compteCorrection = possession.getCompteCorrection();

    new FluxArgent(
        String.format(
            "Correction.Flux[compteCorrection=%s,%s]", compteCorrection.nom, fluxArgent.nom),
        compteCorrection.getCompte(),
        fluxArgent.getDebut(),
        fluxArgent.getFin(),
        fluxArgent.getDateOperation(),
        fluxArgent.getFluxMensuel());
  }

  private Correction(Possession possession, CompteCorrection compteCorrection) {
    super(possession.nom, possession.t, possession.valeurComptable);
    this.possession = possession;
    this.compteCorrection = compteCorrection;
  }

  @Override
  public Correction projectionFuture(LocalDate tFutur) {
    return new Correction(possession.projectionFuture(tFutur), compteCorrection);
  }

  @Override
  public TypeAgregat typeAgregat() {
    return CORRECTION;
  }
}
