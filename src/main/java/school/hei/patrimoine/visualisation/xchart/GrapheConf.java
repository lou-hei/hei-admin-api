package school.hei.patrimoine.visualisation.xchart;

import lombok.Builder;

@Builder(toBuilder = true)
public record GrapheConf(
    boolean avecTitre,
    boolean avecAgregat,
    boolean avecTresorerie,
    boolean avecImmobilisations,
    boolean avecObligations) {}
