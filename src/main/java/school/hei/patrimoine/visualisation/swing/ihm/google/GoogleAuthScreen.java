package school.hei.patrimoine.visualisation.swing.ihm.google;

import static javax.swing.SwingUtilities.invokeLater;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.*;
import lombok.SneakyThrows;
import school.hei.patrimoine.google.GoogleApi;
import school.hei.patrimoine.google.GoogleApi.GoogleAuthenticationDetails;
import school.hei.patrimoine.visualisation.swing.ihm.component.RoundedBorder;
import school.hei.patrimoine.visualisation.swing.ihm.component.RoundedButton;

public class GoogleAuthScreen extends JFrame {

  private final GoogleApi googleApi;

  /**
   * Global instance of the scopes required by this quickstart. If modifying these scopes, delete
   * your previously saved tokens/ folder.
   */
  public GoogleAuthScreen() {
    RoundedButton signInButton = new RoundedButton("Sign in with Google", loadGoogleLogo());
    signInButton.setBackground(new Color(100, 175, 255));
    signInButton.setForeground(Color.WHITE);
    signInButton.setFocusPainted(false);
    signInButton.setFont(new Font("Arial", Font.BOLD, 16));
    signInButton.setPreferredSize(new Dimension(250, 50));
    signInButton.setBorder(new RoundedBorder(50));

    signInButton.addActionListener(onSignin());

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new GridBagLayout());
    buttonPanel.add(signInButton);

    setTitle("Google Authentication");
    setLayout(new BorderLayout());
    add(buttonPanel, BorderLayout.CENTER);
    getContentPane().setBackground(Color.WHITE);

    setSize(700, 400);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    setVisible(true);

    googleApi = new GoogleApi();
  }

  private ActionListener onSignin() {
    return e -> {
      var authReqRes = handleGoogleSignIn();
      invokeLater(() -> new GoogleSubmitScreen(googleApi, authReqRes));
      dispose();
    };
  }

  private Image loadGoogleLogo() {
    try {
      BufferedImage googleLogo =
          ImageIO.read(Objects.requireNonNull(getClass().getResource("/google_logo.png")));
      return googleLogo.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @SneakyThrows
  private GoogleAuthenticationDetails handleGoogleSignIn() {
    return googleApi.requestAuthentication();
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(GoogleAuthScreen::new);
  }
}
