package school.hei.patrimoine.cas;

import java.util.Set;
import school.hei.patrimoine.modele.Argent;

public record CasSet(Set<Cas> set, Argent objectifFinal) {}
