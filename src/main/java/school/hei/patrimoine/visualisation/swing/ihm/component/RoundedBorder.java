package school.hei.patrimoine.visualisation.swing.ihm.component;

import java.awt.*;
import javax.swing.border.Border;

public class RoundedBorder implements Border {
  private int radius;

  public RoundedBorder(int radius) {
    this.radius = radius;
  }

  @Override
  public Insets getBorderInsets(Component c) {
    return new Insets(radius + 1, radius + 1, radius + 1, radius + 1);
  }

  @Override
  public boolean isBorderOpaque() {
    return true;
  }

  @Override
  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
    g2.dispose();
  }
}
