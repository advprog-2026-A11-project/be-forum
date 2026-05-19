package id.ac.ui.cs.advprog.beforum.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtUserExtractorTest {

  private final JwtUserExtractor extractor = new JwtUserExtractor();

  @Test
  void extractUserIdShouldReturnNullWhenJwtIsNull() {
    assertNull(extractor.extractUserId(null));
  }

  @Test
  void extractUserIdShouldReturnNullForMissingClaim() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("yomu_user_id")).thenReturn(null);

    assertNull(extractor.extractUserId(jwt));
  }

  @Test
  void extractUserIdShouldReturnNullForInvalidUuid() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("yomu_user_id")).thenReturn("not-a-uuid");

    assertNull(extractor.extractUserId(jwt));
  }

  @Test
  void extractUserIdShouldReturnUuidForValidClaim() {
    UUID id = UUID.randomUUID();
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("yomu_user_id")).thenReturn(id.toString());

    assertEquals(id, extractor.extractUserId(jwt));
  }
}
