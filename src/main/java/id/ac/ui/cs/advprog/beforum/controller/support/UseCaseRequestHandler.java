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

  private UUID extractUserId(Jwt jwt) {
    if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
