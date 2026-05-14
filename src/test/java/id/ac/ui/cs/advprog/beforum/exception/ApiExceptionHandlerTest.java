package id.ac.ui.cs.advprog.beforum.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import id.ac.ui.cs.advprog.beforum.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void handleUnauthorizedShouldReturnForbidden() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnauthorized(new UnauthorizedException("denied"));

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("FORBIDDEN", response.getBody().error());
    assertEquals("denied", response.getBody().message());
  }

  @Test
  void handleEntityNotFoundShouldReturnNotFound() {
    ResponseEntity<ErrorResponse> response =
        handler.handleEntityNotFound(new EntityNotFoundException("missing"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("NOT_FOUND", response.getBody().error());
    assertEquals("missing", response.getBody().message());
  }

  @Test
  void handleIllegalArgumentShouldReturnBadRequest() {
    ResponseEntity<ErrorResponse> response =
        handler.handleIllegalArgumentException(new IllegalArgumentException("bad"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().error());
    assertEquals("bad", response.getBody().message());
  }

  @Test
  void handleIllegalStateShouldReturnConflict() {
    ResponseEntity<ErrorResponse> response =
        handler.handleIllegalStateException(new IllegalStateException("conflict"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("CONFLICT", response.getBody().error());
    assertEquals("conflict", response.getBody().message());
  }

  @Test
  void handleOptimisticLockingFailureShouldReturnStaleData() {
    ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailure(
        new ObjectOptimisticLockingFailureException(Object.class, 1L));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("STALE_DATA", response.getBody().error());
    assertEquals(
        "The resource was modified by another user. Please refresh and try again.",
        response.getBody().message());
  }

  @Test
  void handleNoResourceFoundShouldReturnNotFound() {
    ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(
        new NoResourceFoundException(HttpMethod.GET, "/missing"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("NOT_FOUND", response.getBody().error());
    assertEquals("Resource not found", response.getBody().message());
  }

  @Test
  void handleGenericExceptionShouldReturnInternalError() {
    ResponseEntity<ErrorResponse> response =
        handler.handleGenericException(new RuntimeException("boom"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("INTERNAL_ERROR", response.getBody().error());
    assertEquals("An unexpected error occurred", response.getBody().message());
  }

  @Test
  void entityNotFoundExceptionConstructorsShouldSetMessageAndCause() {
    Throwable cause = new RuntimeException("cause");
    EntityNotFoundException ex = new EntityNotFoundException("message", cause);

    assertEquals("message", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void unauthorizedExceptionSecondConstructorShouldSetCause() {
    Throwable cause = new RuntimeException("cause");
    UnauthorizedException ex = new UnauthorizedException("message", cause);

    assertEquals("message", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }
}
