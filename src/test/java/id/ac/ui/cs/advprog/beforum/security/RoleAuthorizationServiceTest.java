package id.ac.ui.cs.advprog.beforum.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class RoleAuthorizationServiceTest {

  private final RoleAuthorizationService service = new RoleAuthorizationService();

  @Test
  void isAdminShouldReturnFalseWhenJwtIsNull() {
    assertFalse(service.isAdmin(null));
  }

  @Test
  void isAdminShouldReturnFalseWhenRoleIsNotAdmin() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("user_role")).thenReturn("USER");

    assertFalse(service.isAdmin(jwt));
  }

  @Test
  void isAdminShouldReturnTrueWhenRoleIsAdmin() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("user_role")).thenReturn("ADMIN");

    assertTrue(service.isAdmin(jwt));
  }
}
