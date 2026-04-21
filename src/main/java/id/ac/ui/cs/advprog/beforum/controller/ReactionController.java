package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.dto.ReactionRequest;
import id.ac.ui.cs.advprog.beforum.dto.ReactionResponse;
import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.service.ReactionService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/messages/{messageId}/reactions", "/api/messages/{messageId}/reactions"})
public class ReactionController {

  private final ReactionService service;

  public ReactionController(ReactionService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ReactionResponse> addReaction(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID messageId,
      @RequestBody ReactionRequest req) {
    String userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Reaction reaction;
    try {
      reaction = service.addReaction(messageId, userId, req.reactionType());
    } catch (IllegalStateException e) {
      return ResponseEntity.status(409).build();
    }
    if (reaction == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toResponse(reaction));
  }

  @DeleteMapping
  public ResponseEntity<Void> removeReaction(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID messageId,
      @RequestBody ReactionRequest req) {
    String userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean removed = service.removeReaction(messageId, userId, req.reactionType());
    if (!removed) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<List<ReactionResponse>> getReactions(@PathVariable UUID messageId) {
    List<Reaction> reactions = service.getReactionsByMessageId(messageId);
    return ResponseEntity.ok(reactions.stream().map(this::toResponse).toList());
  }

  @GetMapping("/counts")
  public ResponseEntity<Map<ReactionType, Long>> getReactionCounts(@PathVariable UUID messageId) {
    Map<ReactionType, Long> counts = service.getReactionCountsByMessageId(messageId);
    return ResponseEntity.ok(counts);
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<ReactionResponse>> getUserReactions(
      @PathVariable UUID messageId,
      @PathVariable String userId) {
    List<Reaction> reactions = service.getUserReactionsOnMessage(messageId, userId);
    return ResponseEntity.ok(reactions.stream().map(this::toResponse).toList());
  }

  private String extractUserId(Jwt jwt) {
    if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
      return null;
    }
    return jwt.getSubject();
  }

  private ReactionResponse toResponse(Reaction reaction) {
    return new ReactionResponse(
        reaction.getId(),
        reaction.getReactionType(),
        reaction.getUserId(),
        reaction.getCreatedAt(),
        reaction.getMessageId());
  }
}
