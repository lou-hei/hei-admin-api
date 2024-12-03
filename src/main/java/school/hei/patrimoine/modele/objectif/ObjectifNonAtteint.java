package school.hei.patrimoine.modele.objectif;

import static java.lang.Double.parseDouble;

public record ObjectifNonAtteint(Objectivable objectivable, Objectif objectif) {
  public String prettyPrint() {
    var aAtteindre = objectif.valeurComptable();
    var devise = aAtteindre.devise();
    var t = objectif.t();
    double aAtteindreMontant = parseDouble(aAtteindre.ppMontant());
    double atteintMontant =
        parseDouble(objectivable.valeurAObjectifT(t).convertir(devise, t).ppMontant());
    return String.format(
        "ObjectifNonAtteint[%s, t=%s, devise=%s, objectif=%s, atteint=%s, diff=%s]",
        objectivable.nom(),
        t,
        devise.symbole(),
        aAtteindreMontant,
        atteintMontant,
        aAtteindreMontant - atteintMontant);
  }
}
