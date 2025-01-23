package school.hei.haapi.endpoint.rest.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfigurer implements WebMvcConfigurer {
  private final String frontendUrl;
  private final String casdoorUrl;

  public CorsConfigurer(
      @Value("${casdoor.redirect-url}") String redirectUrl,
      @Value("${casdoor.endpoint}") String casdoorUrl) {
    this.frontendUrl = parseOrigin(redirectUrl);
    this.casdoorUrl = parseOrigin(casdoorUrl);
  }

  private String parseOrigin(String url) {
    int protocol = url.startsWith("https://") ? 5 : 4;
    int slash = url.indexOf('/', protocol + 3);
    return slash == -1 ? url : url.substring(0, slash);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedMethods("*")
        .allowedOrigins(frontendUrl, casdoorUrl)
        .allowCredentials(true);
  }
}
