package school.hei.patrimoine.visualisation.xchart;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_MITER;

import java.awt.*;
import lombok.Getter;

public record StyleSerie(Color color, SerieWidth width, Stroke stroke, boolean withMarker) {

  @Getter
  enum SerieWidth {
    NORMAL(2),
    FAT(4),
    THIN(1);

    private final float value;

    SerieWidth(float value) {
      this.value = value;
    }
  }

  @Getter
  enum Stroke {
    DASH(new BasicStroke(3.0f, CAP_BUTT, JOIN_MITER, 10.0f, new float[] {8.0f}, 0.0f)),
    CONTINUOUS(new BasicStroke());

    private final BasicStroke value;

    Stroke(BasicStroke value) {
      this.value = value;
    }
  }
}
