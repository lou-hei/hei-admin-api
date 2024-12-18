package school.hei.patrimoine.visualisation.swing.ihm.google;

import static java.awt.BorderLayout.CENTER;
import static java.awt.Color.RED;
import static java.awt.Font.BOLD;
import static java.awt.Font.PLAIN;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static javax.swing.SwingConstants.LEFT;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import static school.hei.patrimoine.google.GoogleApi.*;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.cas.CasSet;
import school.hei.patrimoine.cas.CasSetAnalyzer;
import school.hei.patrimoine.compiler.*;
import school.hei.patrimoine.google.GoogleApi;
import school.hei.patrimoine.google.GoogleApi.GoogleAuthenticationDetails;
import school.hei.patrimoine.google.GoogleDocsLinkIdParser;
import school.hei.patrimoine.google.GoogleDriveLinkIdParser;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.visualisation.swing.ihm.MainIHM;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.GoogleLinkList;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.NamedID;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.NamedSnippet;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.NamedString;

@Slf4j
public class GoogleLinkVerifierScreen {
  private final JFrame inputFrame;
  private final JPanel inputPanel;
  private final List<JTextField> inputFields;
  private final GoogleDocsLinkIdInputVerifier docslinkIdInputVerifier =
      new GoogleDocsLinkIdInputVerifier();
  private final GoogleDriveLinkIdInputVerifier drivelinkIdInputVerifier =
      new GoogleDriveLinkIdInputVerifier();
  private final GoogleDocsLinkIdParser docsLinkIdParser = new GoogleDocsLinkIdParser();
  private final GoogleDriveLinkIdParser driveLinkIdParser = new GoogleDriveLinkIdParser();
  private final GoogleApi googleApi;
  private final GoogleAuthenticationDetails authDetails;
  private final GoogleLinkList<NamedString> linksData;
  private int inputYPosition;
  private final File driveDirectory;

  public GoogleLinkVerifierScreen(
      GoogleApi googleApi,
      GoogleAuthenticationDetails authDetails,
      GoogleLinkList<NamedString> linksData) {
    this.googleApi = googleApi;
    this.authDetails = authDetails;
    this.linksData = linksData;
    inputFrame = newInputFrame();
    inputPanel = new JPanel(new GridBagLayout());
    inputFields = new ArrayList<>();
    inputYPosition = 2;
    this.driveDirectory = new File(DOWNLOADS_DIRECTORY_PATH);

    addButtons();
    addInputFieldsFromData();
    configureInputFrame();
  }

  private void configureInputFrame() {
    inputFrame.getContentPane().add(inputPanel);
    inputFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    inputFrame.pack();
    inputFrame.setLocationRelativeTo(null);
  }

  private JFrame newInputFrame() {
    var inputFrame = new JFrame("Google Docs Verifier");
    inputFrame.setSize(1200, 1000);
    inputFrame.setResizable(true);
    inputFrame.setVisible(true);
    return inputFrame;
  }

  private void addButtons() {
    var submitButton = newSubmitButton();
    var returnButton = newReturnButton();

    var buttonTitle = new JLabel("Submit Your Google Docs Links:");
    buttonTitle.setFont(new Font("Arial", BOLD, 24));
    buttonTitle.setHorizontalAlignment(SwingConstants.CENTER);

    var buttonPanel = new JPanel();
    buttonPanel.add(returnButton);
    buttonPanel.add(submitButton);
    buttonPanel.setOpaque(false);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(10, 0, 10, 0);
    gbc.anchor = GridBagConstraints.CENTER;
    inputPanel.add(buttonTitle, gbc);

    gbc.gridy = 1;
    inputPanel.add(buttonPanel, gbc);
  }

  private JButton newSubmitButton() {
    var submitButton = new JButton("Submit");
    submitButton.setPreferredSize(new Dimension(200, 50));
    submitButton.setFont(new Font("Arial", BOLD, 18));
    submitButton.setFocusPainted(false);
    submitButton.addActionListener(e -> loadDataInBackground());
    return submitButton;
  }

  private JButton newReturnButton() {
    var returnButton = new JButton("Return");
    returnButton.setPreferredSize(new Dimension(200, 50));
    returnButton.setFont(new Font("Arial", BOLD, 18));
    returnButton.setFocusPainted(false);
    returnButton.addActionListener(returnToPreviousScreen());
    return returnButton;
  }

  private void addInputFieldsFromData() {
    addNewGoogleDocsTextFields(linksData.docsLinkList());
    addNewGoogleDriveTextFields(linksData.driveLinkList());
  }

  private void addNewGoogleDocsTextFields(List<NamedString> docsLinks) {
    docsLinks.forEach(this::addDocsLinkTextField);
  }

  private void addNewGoogleDriveTextFields(List<NamedString> driveLinks) {
    driveLinks.forEach(this::addDriveLinkTextField);
  }

  private GridBagConstraints createGridBagConstraints() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.fill = HORIZONTAL;
    gbc.insets = new Insets(10, 50, 10, 50);
    return gbc;
  }

  private void addDocsLinkTextField(NamedString linkData) {
    GridBagConstraints gbc = createGridBagConstraints();

    JLabel nameLabel = new JLabel(linkData.name());
    nameLabel.setFont(new Font("Arial", BOLD, 18));
    nameLabel.setHorizontalAlignment(LEFT);

    gbc.gridy = nextYPosition();
    inputPanel.add(nameLabel, gbc);

    JTextField newField = newGoogleDocsLinkTextField(linkData.value());
    inputFields.add(newField);

    JScrollPane scrollPane = new JScrollPane(newField);
    gbc.gridy = nextYPosition();
    inputPanel.add(scrollPane, gbc);
  }

  private void addDriveLinkTextField(NamedString linkData) {
    GridBagConstraints gbc = createGridBagConstraints();

    JLabel nameLabel = new JLabel(linkData.name());
    nameLabel.setFont(new Font("Arial", BOLD, 18));
    nameLabel.setHorizontalAlignment(LEFT);

    gbc.gridy = nextYPosition();
    inputPanel.add(nameLabel, gbc);

    JTextField newField = newGoogleDriveLinkTextField(linkData.value());
    inputFields.add(newField);

    JScrollPane scrollPane = new JScrollPane(newField);
    gbc.gridy = nextYPosition();
    inputPanel.add(scrollPane, gbc);
  }

  private int nextYPosition() {
    return inputYPosition++;
  }

  private JTextField newGoogleDocsLinkTextField(String initialValue) {
    var textField = new JTextField(70);
    textField.setInputVerifier(docslinkIdInputVerifier);
    textField.setFont(new Font("Arial", PLAIN, 16));
    textField.setText(initialValue);
    docslinkIdInputVerifier.verify(textField);
    return textField;
  }

  private JTextField newGoogleDriveLinkTextField(String initialValue) {
    var textField = new JTextField(70);
    textField.setInputVerifier(drivelinkIdInputVerifier);
    textField.setFont(new Font("Arial", PLAIN, 16));
    textField.setText(initialValue);
    drivelinkIdInputVerifier.verify(textField);
    return textField;
  }

  private ActionListener returnToPreviousScreen() {
    return e -> {
      invokeLater(() -> new GoogleSubmitScreen(googleApi, authDetails));
      inputFrame.setVisible(false);
    };
  }

  private void loadDataInBackground() {
    var loadingDialog = new JDialog(inputFrame, "Processing", true);
    var loadingLabel = new JLabel("Processing, please wait...");
    loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
    loadingDialog.getContentPane().add(loadingLabel, CENTER);
    loadingDialog.setSize(300, 100);
    loadingDialog.setLocationRelativeTo(inputFrame);

    SwingWorker<List<Patrimoine>, Void> worker =
        new SwingWorker<>() {
          @Override
          protected List<Patrimoine> doInBackground() {
            var ids = extractInputIds();
            List<NamedSnippet> codePatrimoinesVisualisables = new ArrayList<>();
            List<Patrimoine> patrimoinesVisualisables = new ArrayList<>();

            resetIfExist(DOWNLOADS_DIRECTORY_PATH);
            var patrimoineJarId = driveLinkIdParser.apply(PATRIMOINE_JAR_URL);
            System.out.println(patrimoineJarId);

            googleApi.downloadJarDependencyFile(authDetails, patrimoineJarId);

            for (var id : ids.docsLinkList()) {
              codePatrimoinesVisualisables.add(extractSnippet(id));
            }

            for (var namedId : ids.driveLinkList()) {
              googleApi.downloadDriveFile(authDetails, namedId.id());
            }

            List<File> driveFiles =
                Optional.ofNullable(driveDirectory.listFiles((dir, name) -> name.endsWith(".java")))
                    .map(Arrays::asList)
                    .orElseGet(Collections::emptyList);
            File casSetFile = null;

            for (NamedSnippet codePatrimoine : codePatrimoinesVisualisables) {
              patrimoinesVisualisables.add(compilePatrimoine(codePatrimoine));
            }

            for (File driveFile : driveFiles) {
              if (isCasSetFile(driveFile)) {
                casSetFile = driveFile;
              } else if (isCasFile(driveFile)) {
                compileCas(driveFile.getAbsolutePath());
              } else {
                patrimoinesVisualisables.add(compilePatrimoine(driveFile.getAbsolutePath()));
              }
            }

            if (casSetFile.exists()) {
              compileCasSet(casSetFile.getAbsolutePath());
            }

            return patrimoinesVisualisables;
          }

          @Override
          protected void done() {
            loadingDialog.dispose();
            try {
              final List<Patrimoine> patrimoinesVisualisables = get();
              openResultFrame(patrimoinesVisualisables);
            } catch (InterruptedException | ExecutionException e) {
              Throwable cause = e.getCause();
              if (cause instanceof RuntimeException
                  && cause.getMessage().contains("Objectifs non atteints")) {
                showErrorPage("Objectifs non atteints");
              } else {
                showErrorPage("Veuillez vérifier le contenu de vos documents");
              }
              throw new RuntimeException(e);
            }
          }
        };

    worker.execute();
    loadingDialog.setVisible(true);
  }

  private GoogleLinkList<NamedID> extractInputIds() {
    List<NamedID> docsIds = new ArrayList<>();
    List<NamedID> driveIds = new ArrayList<>();

    for (JTextField field : inputFields) {
      var rawText = field.getText();

      if (rawText.startsWith(
          """
          https://drive.google.com/""")) {
        var parsedId = driveLinkIdParser.apply(rawText.trim());
        String urlName = linksData.driveLinkList().get(inputFields.indexOf(field)).name();
        NamedID namedURL = new NamedID(urlName, parsedId);
        driveIds.add(namedURL);
      } else {
        var parsedId = docsLinkIdParser.apply(rawText.trim());
        String urlName = linksData.docsLinkList().get(inputFields.indexOf(field)).name();
        NamedID namedURL = new NamedID(urlName, parsedId);
        docsIds.add(namedURL);
      }
    }

    return new GoogleLinkList<>(docsIds, driveIds);
  }

  private NamedSnippet extractSnippet(NamedID namedID) {
    var code = googleApi.readDocsContent(authDetails, String.valueOf(namedID.id()));
    return new NamedSnippet(namedID.name(), code);
  }

  private void showErrorPage(String errorMessage) {
    JFrame errorFrame = new JFrame("Erreur");
    errorFrame.setSize(400, 200);
    errorFrame.setLocationRelativeTo(null);
    errorFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JLabel errorLabel = new JLabel(errorMessage, SwingConstants.CENTER);
    errorLabel.setFont(new Font("Arial", BOLD, 16));
    errorLabel.setForeground(RED);

    panel.add(errorLabel, BorderLayout.CENTER);

    errorFrame.getContentPane().add(panel);
    errorFrame.setVisible(true);
  }

  private boolean isCasFile(File file) {
    var casImport = "import school.hei.patrimoine.cas.Cas;";
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(casImport)) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private boolean isCasSetFile(File file) {
    var casSetImport = "import school.hei.patrimoine.cas.CasSet;";
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(casSetImport)) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private Patrimoine compilePatrimoine(NamedSnippet namedSnippet) {
    PatrimoineDocsCompiler patrimoineDocsCompiler = new PatrimoineDocsCompiler();
    String className = new ClassNameExtractor().apply(namedSnippet.snippet());

    return (patrimoineDocsCompiler.apply(className, namedSnippet.snippet()));
  }

  private Patrimoine compilePatrimoine(String filePath) {
    PatrimoineFileCompiler patrimoineFileCompiler = new PatrimoineFileCompiler();

    return (patrimoineFileCompiler.apply(filePath));
  }

  private void compileCas(String filePath) {
    CasFileCompiler casFileCompiler = new CasFileCompiler();

    casFileCompiler.apply(filePath);
  }

  @SneakyThrows
  private void compileCasSet(String filePath) {
    CasFileCompiler casFileCompiler = new CasFileCompiler();
    CasSetAnalyzer casSetAnalyzer = new CasSetAnalyzer();
    var casSet = casFileCompiler.apply(filePath);
    var casSetSupplier = (Supplier<CasSet>) casSet.getDeclaredConstructor().newInstance();

    casSetAnalyzer.accept(casSetSupplier.get());
  }

  private void openResultFrame(List<Patrimoine> patrimoinesVisualisables) {
    invokeLater(() -> new MainIHM(patrimoinesVisualisables));
  }
}
