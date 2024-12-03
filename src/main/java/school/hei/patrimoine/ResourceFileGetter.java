package school.hei.patrimoine;

import java.io.File;
import java.util.function.Function;

public class ResourceFileGetter implements Function<String, File> {

  @Override
  public File apply(String fileName) {
    return new File(this.getClass().getClassLoader().getResource(fileName).getFile());
  }
}
