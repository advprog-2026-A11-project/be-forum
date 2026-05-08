package id.ac.ui.cs.advprog.beforum.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationService;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationValidator;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageRequestValidator;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageResponseMapper;
import id.ac.ui.cs.advprog.beforum.dto.CreateMessageRequest;
import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.security.JwtUserExtractor;
import id.ac.ui.cs.advprog.beforum.service.MessageService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class MessageControllerUnitTest {

  @Mock
  private MessageService service;

  @Test
  void createShouldReturnBadRequestWhenRequestBodyIsNull() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("yomu_user_id")).thenReturn(UUID.randomUUID().toString());

    ResponseEntity<MessageResponse> response = controller().create(jwt, null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(service, never()).createMessage(any(), any(), any());
  }

  @Test
  void createShouldReturnUnauthorizedWhenJwtSubjectIsNull() {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("yomu_user_id")).thenReturn(null);

    ResponseEntity<MessageResponse> response = controller().create(
        jwt,
        new CreateMessageRequest("content", "reading-1"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(service, never()).createMessage(any(), any(), any());
  }

  private MessageController controller() {
    return new MessageController(
        service,
        new JwtUserExtractor(),
        new MessageRequestValidator(),
        new MessageAuthorizationValidator(
            new MessageAuthorizationService(),
            mock()),
        new MessageResponseMapper());
  }
}
