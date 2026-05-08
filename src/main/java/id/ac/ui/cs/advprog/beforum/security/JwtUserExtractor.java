package id.ac.ui.cs.advprog.beforum.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserExtractor {

  private static final String USER_ID_CLAIM = "yomu_user_id";

  public UUID extractUserId(Jwt jwt) {
    if (jwt == null) {
      return null;
    }

    String userIdClaim = jwt.getClaimAsString(USER_ID_CLAIM);
    if (userIdClaim == null || userIdClaim.isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(userIdClaim);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
