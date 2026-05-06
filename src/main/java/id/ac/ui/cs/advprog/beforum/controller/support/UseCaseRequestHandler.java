package id.ac.ui.cs.advprog.beforum.controller.support;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class UseCaseRequestHandler {

  private static final String USER_ID_CLAIM = "yomu_user_id";
  private static final String USER_ROLE_CLAIM = "user_role";
  private static final String ADMIN_ROLE = "ADMIN";

  public UseCaseRequestHandler() {
  }

  public <T> ResponseEntity<T> withAuthenticatedUuid(
      Jwt jwt, Function<UUID, ResponseEntity<T>> useCase) {
    UUID userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return useCase.apply(userId);
  }

  public <T> ResponseEntity<T> require(
      boolean condition, HttpStatus failureStatus, Supplier<ResponseEntity<T>> onSuccess) {
    if (!condition) {
      return ResponseEntity.status(failureStatus).build();
    }
    return onSuccess.get();
  }

  public boolean isAdmin(Jwt jwt) {
    if (jwt == null) {
      return false;
    }
    String userRole = jwt.getClaimAsString(USER_ROLE_CLAIM);
    return ADMIN_ROLE.equals(userRole);
  }

  private UUID extractUserId(Jwt jwt) {
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
