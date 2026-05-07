package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationService;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationValidator;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageRequestValidator;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageResponseMapper;
import id.ac.ui.cs.advprog.beforum.dto.CreateMessageRequest;
import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.exception.EntityNotFoundException;
import id.ac.ui.cs.advprog.beforum.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.security.JwtUserExtractor;
import id.ac.ui.cs.advprog.beforum.service.MessageService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

  private final MessageService service;
  private final JwtUserExtractor userExtractor;
  private final MessageRequestValidator requestValidator;
  private final MessageAuthorizationValidator authorizationValidator;
  private final MessageResponseMapper responseMapper;

  public MessageController(
      MessageService service,
      JwtUserExtractor userExtractor,
      MessageRequestValidator requestValidator,
      MessageAuthorizationValidator authorizationValidator,
      MessageResponseMapper responseMapper) {
    this.service = service;
    this.userExtractor = userExtractor;
    this.requestValidator = requestValidator;
    this.authorizationValidator = authorizationValidator;
    this.responseMapper = responseMapper;
  }

  @PostMapping
  public ResponseEntity<MessageResponse> create(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody CreateMessageRequest req) {
    UUID userId = userExtractor.extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!requestValidator.hasValidReadingId(req)) {
      return ResponseEntity.badRequest().build();
    }

    Message created = service.createMessage(req.content(), req.readingId(), userId);
    return ResponseEntity.ok(responseMapper.toResponse(created));
  }

  @GetMapping
  public ResponseEntity<List<MessageResponse>> list(
      @RequestParam(required = false) String readingId) {
    return ResponseEntity.ok(responseMapper.toResponses(service.listMessages(readingId)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<MessageResponse> getById(@PathVariable UUID id) {
    Message message = service.findByIdWithReplies(id);
    if (message == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(responseMapper.toResponse(message));
  }

  @PutMapping("/{id}")
  public ResponseEntity<MessageResponse> update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody CreateMessageRequest req) {
    UUID userId = userExtractor.extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message message = service.findById(id);
    if (message == null) {
      return ResponseEntity.notFound().build();
    }

    authorizationValidator.validateCanUpdate(message, userId, jwt);

    Message updated = service.updateMessage(id, req.content());
    if (updated == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(responseMapper.toResponse(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id) {
    UUID userId = userExtractor.extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message message = service.findById(id);
    if (message == null) {
      return ResponseEntity.notFound().build();
    }

    authorizationValidator.validateCanDelete(message, userId, jwt);

    service.deleteMessage(id);
    return ResponseEntity.noContent().build();
  }
}
