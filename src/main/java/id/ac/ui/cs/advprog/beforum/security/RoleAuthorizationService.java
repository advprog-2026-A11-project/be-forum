package id.ac.ui.cs.advprog.beforum.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class RoleAuthorizationService {

  private static final String USER_ROLE_CLAIM = "user_role";
  private static final String ADMIN_ROLE = "ADMIN";

  public boolean isAdmin(Jwt jwt) {
    if (jwt == null) {
      return false;
    }
    String userRole = jwt.getClaimAsString(USER_ROLE_CLAIM);
    return ADMIN_ROLE.equals(userRole);
  }
}
