package school.hei.patrimoine.google;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleDocsLinkIdParser implements Function<String, String> {
  public static final Pattern GOOGLE_DOCS_ID_PATTERN =
      Pattern.compile("/document/d/([a-zA-Z0-9-_]+)");

  @Override
  public String apply(String s) {
    Matcher matcher = GOOGLE_DOCS_ID_PATTERN.matcher(s);
    if (!matcher.find()) {
      throw new RuntimeException("Invalid Google Docs Link: " + s);
    }
    return matcher.group(1);
  }
}
