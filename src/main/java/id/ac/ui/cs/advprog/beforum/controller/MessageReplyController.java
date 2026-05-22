package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationService;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationValidator;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages/{parentId}/replies")
public class MessageReplyController {

  private final MessageService service;
  private final MessageQueryCacheService queryCacheService;
  private final JwtUserExtractor userExtractor;
  private final MessageAuthorizationService authorizationService;
  private final MessageAuthorizationValidator authorizationValidator;
  private final MessageResponseMapper responseMapper;
  private final CacheInvalidationService cacheInvalidationService;
  private final CacheMetricsService cacheMetricsService;
  private final ForumMetricsService forumMetricsService;

  @SuppressWarnings("java:S107")
  public MessageReplyController(
      MessageService service,
      MessageQueryCacheService queryCacheService,
      JwtUserExtractor userExtractor,
      MessageAuthorizationService authorizationService,
      MessageAuthorizationValidator authorizationValidator,
      MessageResponseMapper responseMapper,
      CacheInvalidationService cacheInvalidationService,
      CacheMetricsService cacheMetricsService,
      ForumMetricsService forumMetricsService) {
    this.service = service;
    this.queryCacheService = queryCacheService;
    this.userExtractor = userExtractor;
    this.authorizationService = authorizationService;
    this.authorizationValidator = authorizationValidator;
    this.responseMapper = responseMapper;
    this.cacheInvalidationService = cacheInvalidationService;
    this.cacheMetricsService = cacheMetricsService;
    this.forumMetricsService = forumMetricsService;
  }

  @PostMapping
  public ResponseEntity<MessageResponse> createReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @RequestBody CreateMessageRequest req) {
    UUID userId = userExtractor.extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message reply = service.createReply(parentId, req.content(), userId);
    if (reply == null) {
      return ResponseEntity.notFound().build();
    }
    cacheInvalidationService.evictForMessageMutation(reply);
    forumMetricsService.incrementCreated();
    return ResponseEntity.ok(responseMapper.toResponse(reply));
  }

  @GetMapping
  public ResponseEntity<List<MessageResponse>> getReplies(@PathVariable UUID parentId) {
    cacheMetricsService.recordRepliesAccess(parentId);
    List<MessageResponse> replies = queryCacheService.getRepliesDto(parentId);
    if (replies == null) {
      return ResponseEntity.notFound().build();
    }
    forumMetricsService.incrementFetched();
    return ResponseEntity.ok(replies);
  }

  @PutMapping("/{replyId}")
  public ResponseEntity<MessageResponse> updateReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @PathVariable UUID replyId,
      @RequestBody CreateMessageRequest req) {
    UUID userId = userExtractor.extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message reply = service.findById(replyId);
    if (reply == null || !authorizationService.isReplyOfParent(reply, parentId)) {
      return ResponseEntity.notFound().build();
    }

    authorizationValidator.validateCanUpdate(reply, userId, jwt);

    Message updated = service.updateMessage(replyId, req.content());
    if (updated == null) {
      return ResponseEntity.notFound().build();
    }
    cacheInvalidationService.evictForMessageMutation(updated);
    forumMetricsService.incrementUpdated();
    return ResponseEntity.ok(responseMapper.toResponse(updated));
  }

  @DeleteMapping("/{replyId}")
  public ResponseEntity<Void> deleteReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @PathVariable UUID replyId) {
    UUID userId = userExtractor.extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message reply = service.findById(replyId);
    if (reply == null || !authorizationService.isReplyOfParent(reply, parentId)) {
      return ResponseEntity.notFound().build();
    }

    authorizationValidator.validateCanDelete(reply, userId, jwt);

    service.deleteMessage(replyId);
    cacheInvalidationService.evictForMessageMutation(reply);
    forumMetricsService.incrementDeleted();
    return ResponseEntity.noContent().build();
  }
}
