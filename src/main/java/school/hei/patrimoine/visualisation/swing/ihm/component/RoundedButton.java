package school.hei.patrimoine.visualisation.swing.ihm.component;

import java.awt.*;
import javax.swing.*;

public class RoundedButton extends JButton {
  private Image logo;

  public RoundedButton(String text, Image logo) {
    super(text);
    this.logo = logo;
    setContentAreaFilled(false);
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2.setColor(getBackground());
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 50, 50);
    g2.setColor(getForeground());
    FontMetrics fm = g2.getFontMetrics();

    int textX = (int) ((getWidth() - fm.stringWidth(getText())) / 2.6);
    int textY = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();
    g2.drawString(getText(), textX, textY);

    int logoHeight = 25;
    int logoWidth = 25;

    if (logo != null) {
      int logoX = textX + fm.stringWidth(getText()) + 10;
      int logoY = (int) ((getHeight() - logoHeight) / 2.15);
      g2.drawImage(logo, logoX, logoY, logoWidth, logoHeight, null);
    }

    g2.dispose();
  }
}
