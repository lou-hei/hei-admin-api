package school.hei.haapi.endpoint.rest.security;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.casbin.casdoor.entity.CasdoorUser;
import org.casbin.casdoor.exception.CasdoorAuthException;
import org.casbin.casdoor.service.CasdoorAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import school.hei.haapi.endpoint.rest.security.casdoorAuthentication.model.CustomUserDetails;
import school.hei.haapi.endpoint.rest.security.model.Principal;
import school.hei.haapi.model.User;
import school.hei.haapi.service.UserService;

@Component
@RequiredArgsConstructor
// @Slf4j
public class CasdoorAuthProvider extends AbstractUserDetailsAuthenticationProvider {
  private static final String BEARER_PREFIX = "Bearer ";
  private final UserService userService;
  private final CasdoorAuthService casdoorAuthService;

  @Value("${CASDOOR_ORGANIZATION_NAME}")
  String casdoorOrganizationName;

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken token) {
    // nothing
  }

  @Override
  protected UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    String bearer = getBearer(usernamePasswordAuthenticationToken);

    if (bearer == null) {
      throw new UsernameNotFoundException("Bad credentials");
    }

    CasdoorUser casdoorUser = null;
    try {
      casdoorUser = casdoorAuthService.parseJwtToken(bearer);
    } catch (CasdoorAuthException exception) {
      logger.error("casdoor auth exception", exception);
      throw new UsernameNotFoundException("Bad credentials"); // / TODO: custom error message
    }
    boolean hasRole =
        casdoorUser.getRoles().stream()
            .anyMatch(
                role ->
                    Arrays.stream(User.Role.values())
                        .anyMatch(
                            userRole ->
                                role.getName().equalsIgnoreCase(userRole.name())
                                    && role.getOwner().equals(casdoorOrganizationName)));
    if (!hasRole) {
      logger.error(
          "casdoor auth exception",
          new Throwable("User with email " + casdoorUser.getEmail() + " don't have correct role"));
      throw new UsernameNotFoundException("Bad credentials");
    }

    return new Principal(userService.getByEmail(casdoorUser.getEmail()), bearer);
  }

  private String getBearer(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String) || !((String) tokenObject).startsWith(BEARER_PREFIX)) {
      return null;
    }
    return ((String) tokenObject).substring(BEARER_PREFIX.length()).trim();
  }

  public static CustomUserDetails getPrincipal() {
    SecurityContext context = SecurityContextHolder.getContext();
    Authentication authentication = context.getAuthentication();
    return (CustomUserDetails) authentication.getPrincipal();
  }
}
