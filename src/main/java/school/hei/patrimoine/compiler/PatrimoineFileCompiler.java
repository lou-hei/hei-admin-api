package school.hei.patrimoine.compiler;

import static javax.tools.ToolProvider.getSystemJavaCompiler;
import static school.hei.patrimoine.google.GoogleApi.COMPILE_DIR_NAME;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import school.hei.patrimoine.modele.Patrimoine;

public class PatrimoineFileCompiler implements Function<String, Patrimoine> {

  static {
    File tokensDirectory = new File(COMPILE_DIR_NAME);
    if (!tokensDirectory.exists()) {
      tokensDirectory.mkdirs();
    }
  }

  @SneakyThrows
  @Override
  public Patrimoine apply(String filePath) {
    var ioDirPath = Path.of(COMPILE_DIR_NAME);
    var sourcePath = Path.of(filePath);
    var className = getClassNameFromPath(filePath);

    compile(ioDirPath, sourcePath);

    var dynamicClass = loadClass(className, ioDirPath);
    var patrimoineSupplier =
        (Supplier<Patrimoine>) dynamicClass.getDeclaredConstructor().newInstance();
    return patrimoineSupplier.get();
  }

  @SneakyThrows
  private Class<?> loadClass(String className, Path ioDirPath) {
    var classLoader = URLClassLoader.newInstance(new URL[] {ioDirPath.toUri().toURL()});
    boolean INITIALIZE_CLASS = true;

    return Class.forName(className, INITIALIZE_CLASS, classLoader);
  }

  private static String getClassNameFromPath(String filePath) {
    var path = Path.of(filePath);
    String fileName = path.getFileName().toString();
    return fileName.substring(0, fileName.lastIndexOf('.'));
  }

  private void compile(Path ioDirPath, Path sourcePath) {
    var compiler = getSystemJavaCompiler();
    int result =
        compiler.run(
            null,
            null,
            null,
            "-d",
            ioDirPath.toFile().getAbsolutePath(),
            sourcePath.toFile().getAbsolutePath());

    if (result != 0) {
      throw new RuntimeException("Compilation failed. Error code=" + result);
    }
  }
}
