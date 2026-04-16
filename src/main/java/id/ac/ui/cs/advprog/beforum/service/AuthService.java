package id.ac.ui.cs.advprog.beforum.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private final RestClient restClient;

  public AuthService(
      RestClient.Builder builder,
      @Value("${auth.backend-url}") String authBackendUrl) {
    this.restClient = builder.baseUrl(authBackendUrl).build();
  }

  public AuthenticatedUser requireAuthenticatedUser(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()
        || !authorizationHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
    }

    try {
      AuthMeResponse me = restClient.get()
          .uri("/api/auth/me")
          .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
          .retrieve()
          .body(AuthMeResponse.class);

      if (me == null || me.profile == null || me.profile.id == null || me.profile.id.isBlank()) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid auth profile");
      }
      if (Boolean.FALSE.equals(me.profile.isActive)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inactive user");
      }

      UUID userId = UUID.fromString(me.profile.id);
      String role = me.profile.role != null ? me.profile.role : me.role;
      if (role == null || role.isBlank()) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid role");
      }

      return new AuthenticatedUser(userId, role.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid auth payload", ex);
    } catch (RestClientResponseException ex) {
      HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
      if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized", ex);
      }
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Auth backend unavailable", ex);
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Auth backend unavailable", ex);
    }
  }

  public record AuthenticatedUser(UUID userId, String role) {
    public boolean isAdmin() {
      return "ADMIN".equals(role);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class AuthMeResponse {
    public String role;
    public Profile profile;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Profile {
    public String id;
    public String role;
    public Boolean isActive;
  }
}
