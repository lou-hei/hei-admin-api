package school.hei.patrimoine.cas.example;

import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.time.Month.MAY;
import static java.util.Calendar.JUNE;
import static school.hei.patrimoine.modele.Argent.euro;
import static school.hei.patrimoine.modele.Devise.EUR;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Set;
import java.util.function.Supplier;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.Personne;
import school.hei.patrimoine.modele.possession.AchatMaterielAuComptant;
import school.hei.patrimoine.modele.possession.Compte;
import school.hei.patrimoine.modele.possession.FluxArgent;
import school.hei.patrimoine.modele.possession.GroupePossession;
import school.hei.patrimoine.modele.possession.Materiel;
import school.hei.patrimoine.modele.possession.TransfertArgent;

public class PatrimoineRicheSupplier implements Supplier<Patrimoine> {

  @Override
  public Patrimoine get() {
    var ilo = new Personne("Cresus");
    var au13mai24 = LocalDate.of(2024, MAY, 13);
    var compteCourant = new Compte("BP", au13mai24.minusDays(1), au13mai24, euro(13_410));
    new FluxArgent(
        "Salaire",
        compteCourant,
        LocalDate.of(2023, JANUARY, 1),
        LocalDate.of(2026, DECEMBER, 31),
        3,
        euro(4_800));
    var trainDeVie =
        new GroupePossession(
            "Train de vie",
            EUR,
            au13mai24,
            Set.of(
                new FluxArgent(
                    "Loyer",
                    compteCourant,
                    LocalDate.of(2023, JANUARY, 1),
                    LocalDate.of(2026, DECEMBER, 31),
                    27,
                    euro(-1_450)),
                new FluxArgent(
                    "Courses",
                    compteCourant,
                    LocalDate.of(2023, JANUARY, 1),
                    LocalDate.of(2026, DECEMBER, 31),
                    1,
                    euro(-1_100))));

    var voiture =
        new AchatMaterielAuComptant(
            "Voiture", LocalDate.of(2025, JUNE, 4), euro(22_450), -0.4, compteCourant);
    var mac = new Materiel("MacBook Pro", au13mai24, au13mai24, euro(2_000), -0.9);

    var compteEpargne = new Compte("CE", LocalDate.of(2025, Calendar.SEPTEMBER, 7), euro(0));
    new TransfertArgent(
        "Salaire",
        compteCourant,
        compteEpargne,
        LocalDate.of(2025, DECEMBER, 1),
        LocalDate.of(2026, Calendar.JULY, 27),
        3,
        euro(3_200));

    return Patrimoine.of(
        "Riche",
        EUR,
        au13mai24,
        ilo,
        Set.of(compteCourant, compteEpargne, trainDeVie, voiture, mac));
  }
}
