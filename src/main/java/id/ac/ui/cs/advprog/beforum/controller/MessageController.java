package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationValidator;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageRequestValidator;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageResponseMapper;
import id.ac.ui.cs.advprog.beforum.dto.CreateMessageRequest;
import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.security.JwtUserExtractor;
import id.ac.ui.cs.advprog.beforum.service.CacheInvalidationService;
import id.ac.ui.cs.advprog.beforum.service.CacheMetricsService;
import id.ac.ui.cs.advprog.beforum.service.ForumMetricsService;
import id.ac.ui.cs.advprog.beforum.service.MessageQueryCacheService;
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
  private final MessageQueryCacheService queryCacheService;
  private final JwtUserExtractor userExtractor;
  private final MessageRequestValidator requestValidator;
  private final MessageAuthorizationValidator authorizationValidator;
  private final MessageResponseMapper responseMapper;
  private final CacheInvalidationService cacheInvalidationService;
  private final CacheMetricsService cacheMetricsService;
  private final ForumMetricsService forumMetricsService;

  public MessageController(
      MessageService service,
      MessageQueryCacheService queryCacheService,
      JwtUserExtractor userExtractor,
      MessageRequestValidator requestValidator,
      MessageAuthorizationValidator authorizationValidator,
      MessageResponseMapper responseMapper,
      CacheInvalidationService cacheInvalidationService,
      CacheMetricsService cacheMetricsService,
      ForumMetricsService forumMetricsService) {
    this.service = service;
    this.queryCacheService = queryCacheService;
    this.userExtractor = userExtractor;
    this.requestValidator = requestValidator;
    this.authorizationValidator = authorizationValidator;
    this.responseMapper = responseMapper;
    this.cacheInvalidationService = cacheInvalidationService;
    this.cacheMetricsService = cacheMetricsService;
    this.forumMetricsService = forumMetricsService;
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
    cacheInvalidationService.evictForNewMessage(created);
    forumMetricsService.incrementCreated();
    return ResponseEntity.ok(responseMapper.toResponse(created));
  }

  @GetMapping
  public ResponseEntity<List<MessageResponse>> list(
      @RequestParam(required = false) String readingId) {
    cacheMetricsService.recordListAccess(readingId);
    forumMetricsService.incrementFetched();
    return ResponseEntity.ok(queryCacheService.listMessagesDto(readingId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<MessageResponse> getById(@PathVariable UUID id) {
    cacheMetricsService.recordDetailAccess(id);
    MessageResponse response = queryCacheService.getMessageDetailDto(id);
    if (response == null) {
      return ResponseEntity.notFound().build();
    }
    forumMetricsService.incrementFetched();
    return ResponseEntity.ok(response);
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
    cacheInvalidationService.evictForMessageMutation(updated);
    forumMetricsService.incrementUpdated();
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
    cacheInvalidationService.evictForMessageMutation(message);
    forumMetricsService.incrementDeleted();
    return ResponseEntity.noContent().build();
  }
}
