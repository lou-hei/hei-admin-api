package school.hei.patrimoine.google;

import static com.google.api.services.docs.v1.DocsScopes.DOCUMENTS_READONLY;
import static com.google.api.services.drive.DriveScopes.DRIVE_READONLY;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.ParagraphElement;
import com.google.api.services.docs.v1.model.StructuralElement;
import com.google.api.services.docs.v1.model.TextRun;
import com.google.api.services.drive.Drive;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.compiler.ClassNameExtractor;

@Slf4j
public class GoogleApi {
  public record GoogleAuthenticationDetails(NetHttpTransport httpTransport, Credential credential) {

    public boolean isTokenExpired() {
      Long expiresInSeconds = credential.getExpiresInSeconds();

      return expiresInSeconds == null || expiresInSeconds <= 0;
    }
  }

  private static final String APPLICATION_NAME = "patrimoine";

  /** Global instance of the JSON factory. */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  private static final String USER_HOME = System.getProperty("user.home");

  /** Directory to store authorization tokens for this application. */
  private static final String TOKENS_DIRECTORY_PATH = USER_HOME + "/.patrimoine/google/tokens";

  static {
    File tokensDirectory = new File(TOKENS_DIRECTORY_PATH);
    if (!tokensDirectory.exists()) {
      tokensDirectory.mkdirs();
    }
  }

  private static final List<String> SCOPES = List.of(DOCUMENTS_READONLY, DRIVE_READONLY);
  private static final String CREDENTIALS_FILE_PATH = USER_HOME + "/.patrimoine/google/client.json";

  public static final String DOWNLOADS_DIRECTORY_PATH = USER_HOME + "/Downloads/drive";

  public static final String PATRIMOINE_JAR_URL =
      "https://drive.google.com/file/d/1Gm6MpMfWwQbdrk6nqFm3qyDd0GTURK2r/view?usp=drive_link";

  public static final String COMPILE_DIR_NAME = USER_HOME + "/.patrimoine/compile";

  private static final String PATRIMOINE_JAR_NAME = "patrimoine-1.0-SNAPSHOT.jar";

  public static final String PATRIMOINE_JAR_PATH =
      String.valueOf(Path.of(DOWNLOADS_DIRECTORY_PATH).resolve(PATRIMOINE_JAR_NAME));

  static {
    resetIfExist(DOWNLOADS_DIRECTORY_PATH);
  }

  /**
   * Creates an authorized Credential object.
   *
   * @param HTTP_TRANSPORT The network HTTP Transport.
   * @return An authorized Credential object.
   * @throws IOException If the credentials.json file cannot be found.
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
      throws IOException {
    // Load client secrets.
    InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    // returns an authorized Credential object.
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  @SneakyThrows
  public GoogleAuthenticationDetails requestAuthentication() {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    final var currentCredentials = getCredentials(HTTP_TRANSPORT);
    var creds = new GoogleAuthenticationDetails(HTTP_TRANSPORT, currentCredentials);
    if (creds.isTokenExpired()) {
      File tokenDirectory = new File(TOKENS_DIRECTORY_PATH);

      // Delete the old stored token
      if (tokenDirectory.exists() && tokenDirectory.isDirectory()) {
        for (File file : Objects.requireNonNull(tokenDirectory.listFiles())) {
          if (file.isFile()) {
            file.delete();
          }
        }
      }
    }
    final var newCredentials = getCredentials(HTTP_TRANSPORT);
    return new GoogleAuthenticationDetails(HTTP_TRANSPORT, newCredentials);
  }

  public String readDocsContent(GoogleAuthenticationDetails authDetails, String docId) {
    Docs service =
        new Docs.Builder(authDetails.httpTransport(), JSON_FACTORY, authDetails.credential())
            .setApplicationName(APPLICATION_NAME)
            .build();
    try {
      Document doc = service.documents().get(docId).execute();
      List<StructuralElement> bodyElements = doc.getBody().getContent();

      StringBuilder combinedContent = new StringBuilder();

      // Iterate through the body elements
      for (StructuralElement element : bodyElements) {
        if (element.getParagraph() != null) {
          // For each paragraph, get the text content
          List<ParagraphElement> elements = element.getParagraph().getElements();
          for (ParagraphElement e : elements) {
            TextRun textRun = e.getTextRun();
            if (textRun != null && textRun.getContent() != null) {
              combinedContent.append(textRun.getContent());
            }
          }
        }
      }

      return combinedContent.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void resetIfExist(String directoryPath) {
    Path path = Paths.get(directoryPath);
    File directory = path.toFile();

    try {
      // Deletes existing directory if present
      if (directory.exists()) {
        Files.walk(path).map(Path::toFile).forEach(File::delete);
      }
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new RuntimeException("Directory reset error : " + directoryPath, e);
    }
  }

  public void downloadDriveFile(GoogleAuthenticationDetails authDetails, String fileId) {
    Drive driveService =
        new Drive.Builder(authDetails.httpTransport(), JSON_FACTORY, authDetails.credential())
            .setApplicationName(APPLICATION_NAME)
            .build();

    // Create a temporary file to store downloaded content
    File tempFile =
        new File(System.getProperty("java.io.tmpdir"), "prefix_" + UUID.randomUUID() + ".java");

    try (InputStream inputStream = driveService.files().get(fileId).executeMediaAsInputStream();
        FileOutputStream tempOutputStream = new FileOutputStream(tempFile)) {

      // Download content to temporary file
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        tempOutputStream.write(buffer, 0, bytesRead);
      }

      // Read the contents of the temporary file as a String
      String fileContent = Files.readString(tempFile.toPath());

      // Extract class name
      String className = new ClassNameExtractor().apply(fileContent);

      // Define final file name based on class name
      File finalFile = new File(DOWNLOADS_DIRECTORY_PATH, className + ".java");

      copyFileContent(tempFile, finalFile);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void downloadJarDependencyFile(GoogleAuthenticationDetails authDetails, String fileId) {
    Drive driveService =
        new Drive.Builder(authDetails.httpTransport(), JSON_FACTORY, authDetails.credential())
            .setApplicationName(APPLICATION_NAME)
            .build();

    // Create a temporary file to store downloaded content
    File tempFile =
        new File(System.getProperty("java.io.tmpdir"), "prefix_" + UUID.randomUUID() + ".java");

    try (InputStream inputStream = driveService.files().get(fileId).executeMediaAsInputStream();
        FileOutputStream tempOutputStream = new FileOutputStream(tempFile)) {

      // Download content to temporary file
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        tempOutputStream.write(buffer, 0, bytesRead);
      }

      // Define final file name based on class name
      File finalFile = new File(DOWNLOADS_DIRECTORY_PATH, PATRIMOINE_JAR_NAME);

      copyFileContent(tempFile, finalFile);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void copyFileContent(File sourceFile, File targetFile) {
    byte[] buffer = new byte[1024];
    int bytesRead;

    try (FileOutputStream targetOutputStream = new FileOutputStream(targetFile);
        FileInputStream sourceInputStream = new FileInputStream(sourceFile)) {

      while ((bytesRead = sourceInputStream.read(buffer)) != -1) {
        targetOutputStream.write(buffer, 0, bytesRead);
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to copy file content", e);
    } finally {
      if (sourceFile.exists()) {
        sourceFile.delete();
      }
    }
  }
}
