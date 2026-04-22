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

  private final MessagePrincipalResolver principalResolver;

  public UseCaseRequestHandler(MessagePrincipalResolver principalResolver) {
    this.principalResolver = principalResolver;
  }

  public <T> ResponseEntity<T> withAuthenticatedUuid(
      Jwt jwt, Function<UUID, ResponseEntity<T>> useCase) {
    UUID userId = principalResolver.extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return useCase.apply(userId);
  }

  public <T> ResponseEntity<T> withAuthenticatedSubject(
      Jwt jwt, Function<String, ResponseEntity<T>> useCase) {
    if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return useCase.apply(jwt.getSubject());
  }

  public <T> ResponseEntity<T> require(
      boolean condition, HttpStatus failureStatus, Supplier<ResponseEntity<T>> onSuccess) {
    if (!condition) {
      return ResponseEntity.status(failureStatus).build();
    }
    return onSuccess.get();
  }
}
