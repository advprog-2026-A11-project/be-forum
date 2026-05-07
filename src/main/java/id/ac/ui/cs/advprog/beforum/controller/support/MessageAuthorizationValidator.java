package id.ac.ui.cs.advprog.beforum.controller.support;

import id.ac.ui.cs.advprog.beforum.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.security.RoleAuthorizationService;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class MessageAuthorizationValidator {

  private final MessageAuthorizationService authService;
  private final RoleAuthorizationService roleService;

  public MessageAuthorizationValidator(
      MessageAuthorizationService authService,
      RoleAuthorizationService roleService) {
    this.authService = authService;
    this.roleService = roleService;
  }

  public void validateCanUpdate(Message message, UUID userId, Jwt jwt)
      throws UnauthorizedException {
    if (roleService.isAdmin(jwt)) {
      throw new UnauthorizedException("You're not allowed to edit this message");
    }
    if (!authService.isOwner(message, userId)) {
      throw new UnauthorizedException("You're not allowed to edit this message");
    }
  }

  public void validateCanDelete(Message message, UUID userId, Jwt jwt)
      throws UnauthorizedException {
    // Admins can delete anything
    if (roleService.isAdmin(jwt)) {
      return;
    }

    if (!authService.isOwner(message, userId)) {
      throw new UnauthorizedException("You're not allowed to delete this message");
    }
  }
}
