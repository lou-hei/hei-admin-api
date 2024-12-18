package school.hei.patrimoine.visualisation.xchart;

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.DARK_GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import static java.nio.file.Files.createTempFile;
import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;
import static org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG;
import static org.knowm.xchart.BitmapEncoder.saveBitmapWithDPI;
import static org.knowm.xchart.style.Styler.LegendPosition.OutsideE;
import static org.knowm.xchart.style.markers.SeriesMarkers.NONE;
import static school.hei.patrimoine.modele.possession.TypeAgregat.CORRECTION;
import static school.hei.patrimoine.modele.possession.TypeAgregat.IMMOBILISATION;
import static school.hei.patrimoine.modele.possession.TypeAgregat.OBLIGATION;
import static school.hei.patrimoine.modele.possession.TypeAgregat.PATRIMOINE;
import static school.hei.patrimoine.modele.possession.TypeAgregat.TRESORERIE;
import static school.hei.patrimoine.visualisation.xchart.StyleSerie.SerieWidth.FAT;
import static school.hei.patrimoine.visualisation.xchart.StyleSerie.SerieWidth.NORMAL;
import static school.hei.patrimoine.visualisation.xchart.StyleSerie.SerieWidth.THIN;
import static school.hei.patrimoine.visualisation.xchart.StyleSerie.Stroke.CONTINUOUS;
import static school.hei.patrimoine.visualisation.xchart.StyleSerie.Stroke.DASH;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BiFunction;
import lombok.SneakyThrows;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.theme.MatlabTheme;
import school.hei.patrimoine.modele.evolution.EvolutionPatrimoine;
import school.hei.patrimoine.modele.possession.Possession;

public class GrapheurEvolutionPatrimoine
    implements BiFunction<EvolutionPatrimoine, GrapheConf, File> {

  private static final int DPI = 300;

  private void configureSeries(EvolutionPatrimoine ep, XYChart chart, GrapheConf grapheConf) {
    var serieComptableTemporelle = ep.getSerieComptableTemporelle();
    var dates = serieComptableTemporelle.serieDates();
    var seriesParPossession = serieComptableTemporelle.serieValeursComptablesParPossession();
    seriesParPossession.keySet().stream()
        .sorted(comparing(Possession::nom))
        .forEach(p -> configureSerie(chart, grapheConf, p, dates, seriesParPossession.get(p)));
    addSerie(
        chart,
        "Patrimoine",
        dates,
        serieComptableTemporelle.serieValeursComptablesPatrimoine(),
        new StyleSerie(GREEN, FAT, CONTINUOUS, false));
    addSerie(
        chart,
        "Correction",
        dates,
        serieComptableTemporelle.serieParPossessionsFiltrées(
            p -> CORRECTION.equals(p.typeAgregat())),
        new StyleSerie(MAGENTA, FAT, CONTINUOUS, false));
    addSerie(
        chart,
        "Patrimoines",
        dates,
        serieComptableTemporelle.serieParPossessionsFiltrées(
            p -> PATRIMOINE.equals(p.typeAgregat())),
        new StyleSerie(BLUE, FAT, CONTINUOUS, false));

    if (!grapheConf.avecAgregat()) {
      return;
    }
    if (grapheConf.avecTresorerie()) {
      addSerie(
          chart,
          "Trésorerie",
          dates,
          serieComptableTemporelle.serieParPossessionsFiltrées(
              p -> TRESORERIE.equals(p.typeAgregat())),
          new StyleSerie(RED, FAT, CONTINUOUS, false));
    }
    if (grapheConf.avecImmobilisations()) {
      addSerie(
          chart,
          "Immobilisations",
          dates,
          serieComptableTemporelle.serieParPossessionsFiltrées(
              p -> IMMOBILISATION.equals(p.typeAgregat())),
          new StyleSerie(BLACK, FAT, CONTINUOUS, false));
    }
    if (grapheConf.avecObligations()) {
      addSerie(
          chart,
          "Obligations",
          dates,
          serieComptableTemporelle.serieParPossessionsFiltrées(
              p -> OBLIGATION.equals(p.typeAgregat())),
          new StyleSerie(YELLOW, FAT, DASH, false));
    }
  }

  private void configureSerie(
      XYChart chart,
      GrapheConf grapheConf,
      Possession possession,
      List<LocalDate> dates,
      List<Integer> serie) {
    if (!grapheConf.avecTresorerie() && TRESORERIE.equals(possession.typeAgregat())
        || !grapheConf.avecImmobilisations() && IMMOBILISATION.equals(possession.typeAgregat())
        || !grapheConf.avecObligations() && OBLIGATION.equals(possession.typeAgregat())) {
      return;
    }

    addSerie(chart, possession.nom(), dates, serie, styleSerie(possession));
  }

  private StyleSerie styleSerie(Possession possession) {
    var typeAgregat = possession.typeAgregat();
    if (IMMOBILISATION.equals(typeAgregat)) {
      return new StyleSerie(null, NORMAL, CONTINUOUS, true);
    }

    return OBLIGATION.equals(typeAgregat)
        ? new StyleSerie(null, THIN, DASH, false)
        : new StyleSerie(null, NORMAL, CONTINUOUS, false);
  }

  private void addSerie(
      XYChart chart,
      String nom,
      List<LocalDate> localDates,
      List<Integer> values,
      StyleSerie style) {
    if (values.stream().allMatch(value -> value == 0)) {
      return;
    }

    var dates =
        localDates.stream()
            .map(localDate -> Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .toList();
    var serie = chart.addSeries(nom, dates, values);

    if (!style.withMarker()) {
      serie.setMarker(NONE);
    }
    serie.setMarkerColor(DARK_GRAY);
    serie.setSmooth(true);
    serie.setLineColor(style.color() == null ? color(nom) : style.color());
    serie.setLineWidth(style.width().getValue());
    serie.setLineStyle(style.stroke().getValue());
  }

  private Color color(String nom) {
    var nomEnNb = Arrays.stream(nom.split("")).mapToInt(s -> s.charAt(0)).sum();

    // do not use same value for rgb as it will cause gray-scaled images in most cases
    var r = nomEnNb % 255;
    var g = 255 - r;
    var b = (128 + nomEnNb) % 255;

    return new Color(r, g, b);
  }

  @SneakyThrows
  @Override
  public File apply(EvolutionPatrimoine evolutionPatrimoine, GrapheConf grapheConf) {
    XYChart chart = new XYChartBuilder().width(800).height(600).build();
    if (grapheConf.avecTitre()) {
      var patrimoine = evolutionPatrimoine.getPatrimoine();
      chart.setTitle(String.format("%s (%s)", patrimoine.nom(), patrimoine.getDevise().symbole()));
    }
    configureStyle(chart);
    configureSeries(evolutionPatrimoine, chart, grapheConf);

    var temp = createTempFile(randomUUID().toString(), ".png").toFile();
    saveBitmapWithDPI(chart, temp.getAbsolutePath(), PNG, DPI);
    System.out.println("Image générée: " + temp.getAbsolutePath());

    return temp;
  }

  public File apply(EvolutionPatrimoine ep) {
    return apply(ep, new GrapheConf(false, false, true, true, true));
  }

  private void configureStyle(XYChart chart) {
    var styler = chart.getStyler();
    styler.setPlotBackgroundColor(WHITE);
    styler.setChartBackgroundColor(WHITE);
    styler.setLegendPosition(OutsideE);
    styler.setyAxisTickLabelsFormattingFunction(this::yFormatter);
    styler.setPlotMargin(0);
    styler.setTheme(new MatlabTheme());

    chart.getSeriesMap().values().forEach(serie -> serie.setMarker(NONE));
  }

  private String yFormatter(Double valeurComptable) {
    var decimalFormatter = new DecimalFormat("###,###,###");
    var symbols = decimalFormatter.getDecimalFormatSymbols();
    symbols.setGroupingSeparator(' ');
    decimalFormatter.setDecimalFormatSymbols(symbols);
    return decimalFormatter.format(valeurComptable);
  }
}
