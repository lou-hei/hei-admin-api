package school.hei.patrimoine.modele;

import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import school.hei.patrimoine.modele.possession.PatrimoinePersonnel;

@EqualsAndHashCode
public class Personne implements Serializable /*note(no-serializable)*/ {
  @Accessors(fluent = true)
  @Getter
  private final String nom;

  @EqualsAndHashCode.Exclude private List<Patrimoine> patrimoines = new ArrayList<>();

  public Personne(String nom) {
    this.nom = nom;
  }

  public Patrimoine patrimoine(Devise devise, LocalDate t) {
    return Patrimoine.of(
        nom + " (personnel)",
        devise,
        t,
        this,
        patrimoines.stream()
            .map(patrimoine -> new PatrimoinePersonnel(patrimoine, this))
            .collect(toSet()));
  }

  /*no-public*/ void addPatrimoine(Patrimoine patrimoine) {
    patrimoines.add(patrimoine);
  }
}
