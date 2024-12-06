package school.hei.haapi.service;

import static school.hei.patrimoine.modele.Argent.ariary;
import static school.hei.patrimoine.modele.Devise.MGA;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.haapi.model.*;
import school.hei.haapi.repository.FeeRepository;
import school.hei.patrimoine.modele.Argent;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.Personne;
import school.hei.patrimoine.modele.evolution.EvolutionPatrimoine;
import school.hei.patrimoine.modele.possession.Creance;
import school.hei.patrimoine.modele.possession.Possession;
import school.hei.patrimoine.visualisation.xchart.GrapheurEvolutionPatrimoine;

@Service
@AllArgsConstructor
@Slf4j
public class PatrimoineService {
  private final FeeRepository feeRepository;
  private final GrapheurEvolutionPatrimoine grapheurEvolutionPatrimoine =
      new GrapheurEvolutionPatrimoine();

  public File visualizeUnpaidFees() {
    Instant now = Instant.now();
    List<Fee> unpaidFees = feeRepository.getUnpaidFees(now);
    var hei = new Personne("HEI");
    var zoneId = ZoneId.systemDefault();
    Set<Possession> creances = new HashSet<>();

    LocalDate latestDueDate =
        unpaidFees.stream()
            .map(fee -> fee.getDueDatetime().atZone(zoneId).toLocalDate())
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());

    unpaidFees.forEach(
        fee -> {
          LocalDate localDate = fee.getDueDatetime().atZone(zoneId).toLocalDate();
          Argent totalAmount = ariary(fee.getTotalAmount());
          Creance creance = new Creance(fee.describe(), localDate, totalAmount);
          creances.add(creance);
        });

    var patrimoineEncaissement =
        Patrimoine.of("Frais non encaissés", MGA, LocalDate.now(), hei, creances);
    var evolutionEncaissement =
        new EvolutionPatrimoine(
            "Evolution frais non encaissés",
            patrimoineEncaissement,
            LocalDate.now(),
            latestDueDate);
    return grapheurEvolutionPatrimoine.apply(evolutionEncaissement);
  }
}
