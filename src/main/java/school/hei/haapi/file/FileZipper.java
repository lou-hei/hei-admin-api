package school.hei.haapi.file;

import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

@Component
public class FileZipper implements Function<List<byte[]>, File> {
  private static final String ZIP_FILE_EXTENSION = ".zip";

  @SneakyThrows
  @Override
  public File apply(List<byte[]> fileDataList) {
    File zipFile = File.createTempFile(randomUUID().toString(), ZIP_FILE_EXTENSION, null);
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos)) {
      for (byte[] fileData : fileDataList) {
        File file = File.createTempFile(UUID.randomUUID().toString(), ".pdf");
        FileUtils.writeByteArrayToFile(file, fileData);
        try (FileInputStream fis = new FileInputStream(file)) {
          ZipEntry zipEntry = new ZipEntry(file.getName());
          zipOut.putNextEntry(zipEntry);
          byte[] bytes = new byte[1024];
          int length;
          while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
        }
        file.delete();
      }
    }
    return zipFile;
  }
}
