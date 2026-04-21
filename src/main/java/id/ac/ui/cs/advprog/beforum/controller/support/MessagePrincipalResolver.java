package id.ac.ui.cs.advprog.beforum.controller.support;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class MessagePrincipalResolver {

  public UUID extractUserId(Jwt jwt) {
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
