package school.hei.patrimoine.visualisation.swing.ihm.google;

import static java.awt.BorderLayout.CENTER;
import static java.awt.Font.*;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.google.GoogleApi;
import school.hei.patrimoine.google.GoogleApi.GoogleAuthenticationDetails;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.GoogleLinkList;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.NamedString;

@Slf4j
public class GoogleSubmitScreen {
  private final JFrame inputFrame;
  private final JPanel inputPanel;
  private final JTextArea docsField;
  private final JTextArea driveField;
  private final GoogleDocsLinkIdInputVerifier linkIdInputVerifier =
      new GoogleDocsLinkIdInputVerifier();
  private final GoogleApi googleApi;
  private final GoogleAuthenticationDetails authDetails;

  public GoogleSubmitScreen(GoogleApi googleApi, GoogleAuthenticationDetails authDetails) {
    this.googleApi = googleApi;
    this.authDetails = authDetails;
    inputFrame = newInputFrame();
    inputPanel = new JPanel();
    inputPanel.setLayout(new GridBagLayout());

    docsField = createDocsField();
    driveField = createDriveField();
    addButtons();
    addInitialInput();

    configureInputFrame();
  }

  private JTextArea createDocsField() {
    return new JTextArea(5, 70);
  }

  private JTextArea createDriveField() {
    return new JTextArea(5, 70);
  }

  private void configureInputFrame() {
    inputFrame.getContentPane().add(inputPanel);
    inputFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    inputFrame.pack();
    inputFrame.setLocationRelativeTo(null);
  }

  private JFrame newInputFrame() {
    var inputFrame = new JFrame("Google Docs Submission");
    inputFrame.setSize(1200, 1000);
    inputFrame.setResizable(true);
    inputFrame.setVisible(true);
    return inputFrame;
  }

  private void addButtons() {
    JButton submitButton = newSubmitButton();

    JLabel buttonTitle = new JLabel("Enter Google Links:");
    buttonTitle.setFont(new Font("Arial", BOLD, 24));
    buttonTitle.setHorizontalAlignment(SwingConstants.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(submitButton);
    buttonPanel.setOpaque(false);

    var gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(10, 0, 10, 0);
    gbc.anchor = GridBagConstraints.CENTER;
    inputPanel.add(buttonTitle, gbc);

    gbc.gridy = 1;
    inputPanel.add(buttonPanel, gbc);
  }

  private JButton newSubmitButton() {
    var submitButton = new JButton("Verify");
    submitButton.setPreferredSize(new Dimension(200, 50));
    submitButton.setFont(new Font("Arial", BOLD, 18));
    submitButton.setFocusPainted(false);
    submitButton.addActionListener(e -> loadDataInBackground());
    return submitButton;
  }

  private void addInitialInput() {
    // Google docs field label
    JLabel docsLabel = new JLabel("Google Docs Links");
    docsLabel.setFont(new Font("Arial", CENTER_BASELINE, 14));

    var gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(10, 50, 5, 50);
    inputPanel.add(docsLabel, gbc);

    // Google docs input field
    docsField.setLineWrap(true);
    docsField.setWrapStyleWord(true);
    docsField.setInputVerifier(linkIdInputVerifier);
    docsField.setFont(new Font("Arial", PLAIN, 16));

    gbc.gridy = 3;
    gbc.insets = new Insets(5, 50, 10, 50);
    JScrollPane scrollPane = new JScrollPane(docsField);
    inputPanel.add(scrollPane, gbc);

    // Google drive field label
    JLabel driveLabel = new JLabel("Google Drive Links");
    driveLabel.setFont(new Font("Arial", CENTER_BASELINE, 14));

    gbc.gridy = 4;
    gbc.insets = new Insets(10, 50, 5, 50);
    inputPanel.add(driveLabel, gbc);

    // Google docs input field
    driveField.setLineWrap(true);
    driveField.setWrapStyleWord(true);
    driveField.setInputVerifier(linkIdInputVerifier);
    driveField.setFont(new Font("Arial", PLAIN, 16));

    gbc.gridy = 5;
    gbc.insets = new Insets(5, 50, 10, 50);
    JScrollPane driveScrollPane = new JScrollPane(driveField);
    inputPanel.add(driveScrollPane, gbc);
  }

  private void loadDataInBackground() {
    JDialog loadingDialog = new JDialog(inputFrame, "Processing", true);
    JLabel loadingLabel = new JLabel("Processing, please wait...");
    loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
    loadingDialog.getContentPane().add(loadingLabel, CENTER);
    loadingDialog.setSize(300, 100);
    loadingDialog.setLocationRelativeTo(inputFrame);

    SwingWorker<GoogleLinkList<NamedString>, Void> worker =
        new SwingWorker<>() {
          @Override
          protected GoogleLinkList<NamedString> doInBackground() {
            return extractGoogleLinks();
          }

          @Override
          protected void done() {
            loadingDialog.dispose();
            try {
              final GoogleLinkList<NamedString> inputData = get();
              openResultFrame(inputData, googleApi, authDetails);
            } catch (InterruptedException | ExecutionException e) {
              throw new RuntimeException(e);
            }
          }
        };

    worker.execute();
    loadingDialog.setVisible(true);
  }

  private List<NamedString> extractInputData(List<String> lines) {
    List<NamedString> linkDataList = new ArrayList<>();

    for (String line : lines) {
      String[] parts = line.split(":", 2);

      if (parts.length == 2) {
        String linkName = parts[0].trim();
        String linkValue = parts[1].trim();

        NamedString linkData = new NamedString(linkName, linkValue);
        linkDataList.add(linkData);
      }
    }

    return linkDataList;
  }

  private GoogleLinkList<NamedString> extractGoogleLinks() {
    String docsRawText = docsField.getText();
    String driveRawText = driveField.getText();

    List<String> docsLines = Arrays.asList(docsRawText.split("\n"));
    List<String> driveLines = Arrays.asList(driveRawText.split("\n"));

    List<NamedString> docsLink = extractInputData(docsLines);
    List<NamedString> driveLink = extractInputData(driveLines);

    return new GoogleLinkList<>(docsLink, driveLink);
  }

  private void openResultFrame(
      GoogleLinkList<NamedString> googleLinkList,
      GoogleApi googleApi,
      GoogleAuthenticationDetails authReqRes) {
    invokeLater(() -> new GoogleLinkVerifierScreen(googleApi, authReqRes, googleLinkList));
    inputFrame.dispose();
  }
}
