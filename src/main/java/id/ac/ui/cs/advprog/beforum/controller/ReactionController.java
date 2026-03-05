package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.service.ReactionService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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

  public static record ReactionRequest(String userId, ReactionType reactionType) {
  }

  @PostMapping
  public ResponseEntity<Reaction> addReaction(
      @PathVariable UUID messageId,
      @RequestBody ReactionRequest req) {
    Reaction reaction = service.addReaction(messageId, req.userId(), req.reactionType());
    if (reaction == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(reaction);
  }

  @DeleteMapping
  public ResponseEntity<Void> removeReaction(
      @PathVariable UUID messageId,
      @RequestBody ReactionRequest req) {
    boolean removed = service.removeReaction(messageId, req.userId(), req.reactionType());
    if (!removed) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<List<Reaction>> getReactions(@PathVariable UUID messageId) {
    List<Reaction> reactions = service.getReactionsByMessageId(messageId);
    return ResponseEntity.ok(reactions);
  }

  @GetMapping("/counts")
  public ResponseEntity<Map<ReactionType, Long>> getReactionCounts(@PathVariable UUID messageId) {
    Map<ReactionType, Long> counts = service.getReactionCountsByMessageId(messageId);
    return ResponseEntity.ok(counts);
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<Reaction>> getUserReactions(
      @PathVariable UUID messageId,
      @PathVariable String userId) {
    List<Reaction> reactions = service.getUserReactionsOnMessage(messageId, userId);
    return ResponseEntity.ok(reactions);
  }
}
