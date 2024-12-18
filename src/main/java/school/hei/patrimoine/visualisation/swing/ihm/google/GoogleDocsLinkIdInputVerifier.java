package school.hei.patrimoine.visualisation.swing.ihm.google;

import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static school.hei.patrimoine.google.GoogleDocsLinkIdParser.GOOGLE_DOCS_ID_PATTERN;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

public class GoogleDocsLinkIdInputVerifier extends InputVerifier {
  @Override
  public boolean verify(JComponent input) {
    if (input instanceof JTextField textField) {
      String text = textField.getText();
      if (GOOGLE_DOCS_ID_PATTERN.matcher(text).find()) {
        textField.setBackground(GREEN);
        return true;
      } else {
        textField.setBackground(RED);
        return false;
      }
    }
    return true;
  }
}
