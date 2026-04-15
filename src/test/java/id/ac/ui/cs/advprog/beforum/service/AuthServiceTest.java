package id.ac.ui.cs.advprog.beforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTest {

  private AuthService authService;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    authService = new AuthService(builder, "http://auth-service");
  }

  @Test
  void requireAuthenticatedUserShouldReturnAuthenticatedUser() {
    String userId = "535251d5-a941-49b0-9a04-5b26dc55ec61";
    server.expect(requestTo("http://auth-service/api/auth/me"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer token"))
        .andRespond(withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              """
                {
                  "role": "STUDENT",
                  "profile": {
                    "id": "%s",
                    "role": "ADMIN",
                    "isActive": true
                  }
                }
              """.formatted(userId)));

    AuthService.AuthenticatedUser user = authService.requireAuthenticatedUser("Bearer token");

    assertEquals(userId, user.userId().toString());
    assertEquals("ADMIN", user.role());
    assertEquals(true, user.isAdmin());
  }

  @Test
  void requireAuthenticatedUserShouldRejectMissingBearerToken() {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> authService.requireAuthenticatedUser("token"));
    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void requireAuthenticatedUserShouldRejectInactiveUser() {
    server.expect(requestTo("http://auth-service/api/auth/me"))
        .andRespond(withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              """
                {
                  "profile": {
                    "id": "535251d5-a941-49b0-9a04-5b26dc55ec61",
                    "role": "STUDENT",
                    "isActive": false
                  }
                }
              """));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> authService.requireAuthenticatedUser("Bearer token"));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void requireAuthenticatedUserShouldMapUnauthorizedFromAuthBackend() {
    server.expect(requestTo("http://auth-service/api/auth/me"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> authService.requireAuthenticatedUser("Bearer token"));
    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void requireAuthenticatedUserShouldReturnServiceUnavailableOnAuthError() {
    server.expect(requestTo("http://auth-service/api/auth/me"))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> authService.requireAuthenticatedUser("Bearer token"));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
  }
}
