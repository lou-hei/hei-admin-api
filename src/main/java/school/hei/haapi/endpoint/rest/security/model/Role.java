package school.hei.haapi.endpoint.rest.security.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
  ADMIN,
  MONITOR,
  STUDENT,
  TEACHER,
  STAFF_MEMBER,
  MANAGER,
  ORGANIZER;

  public String getRole() {
    return name();
  }

  public static Role fromValue(String value) {
    for (Role b : Role.values()) {
      if (b.getRole().equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  @Override
  public String getAuthority() {
    return "ROLE_" + getRole();
  }
}
