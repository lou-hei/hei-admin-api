package school.hei.patrimoine.compiler;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassNameExtractor implements Function<String, String> {
  private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("public class (\\w+)");

  @Override
  public String apply(String code) {
    Matcher matcher = CLASS_NAME_PATTERN.matcher(code);
    if (matcher.find()) {
      return matcher.group(1);
    } else {
      throw new IllegalArgumentException("No class name found in the provided code.");
    }
  }
}
