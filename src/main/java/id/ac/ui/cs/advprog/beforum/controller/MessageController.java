package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.dto.CreateMessageRequest;
import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.model.Message;
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
@RequestMapping({"/messages", "/api/messages"})
public class MessageController {

  private final MessageService service;

  public MessageController(MessageService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<MessageResponse> create(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody CreateMessageRequest req) {
    UUID userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (req == null || req.readingId() == null || req.readingId().isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    try {
      Message created = service.createMessage(req.content(), req.readingId(), userId);
      return ResponseEntity.ok(toResponse(created));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping
  public ResponseEntity<List<MessageResponse>> list(@RequestParam(required = false) String readingId) {
    return ResponseEntity.ok(service.listMessages(readingId).stream().map(this::toResponse).toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<MessageResponse> getById(@PathVariable UUID id) {
    Message message = service.findByIdWithReplies(id);
    if (message == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toResponse(message));
  }

  @PutMapping("/{id}")
  public ResponseEntity<MessageResponse> update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody CreateMessageRequest req) {
    UUID userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message found = service.findById(id);
    if (found == null) {
      return ResponseEntity.notFound().build();
    }
    if (!isOwner(found, userId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    Message updated = service.updateMessage(id, req.content());
    if (updated == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toResponse(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id) {
    UUID userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message found = service.findById(id);
    if (found == null) {
      return ResponseEntity.notFound().build();
    }
    if (!isOwner(found, userId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    service.deleteMessage(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{parentId}/replies")
  public ResponseEntity<MessageResponse> createReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @RequestBody CreateMessageRequest req) {
    UUID userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message reply = service.createReply(parentId, req.content(), userId);
    if (reply == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toResponse(reply));
  }

  @GetMapping("/{parentId}/replies")
  public ResponseEntity<List<MessageResponse>> getReplies(@PathVariable UUID parentId) {
    Message parent = service.findById(parentId);
    if (parent == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(service.getReplies(parentId).stream().map(this::toResponse).toList());
  }

  @PutMapping("/{parentId}/replies/{replyId}")
  public ResponseEntity<MessageResponse> updateReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @PathVariable UUID replyId,
      @RequestBody CreateMessageRequest req) {
    UUID userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message reply = service.findById(replyId);
    if (reply == null || reply.getParentId() == null || !reply.getParentId().equals(parentId)) {
      return ResponseEntity.notFound().build();
    }
    if (!isOwner(reply, userId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    Message updated = service.updateMessage(replyId, req.content());
    if (updated == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(toResponse(updated));
  }

  @DeleteMapping("/{parentId}/replies/{replyId}")
  public ResponseEntity<Void> deleteReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @PathVariable UUID replyId) {
    UUID userId = extractUserId(jwt);
    if (userId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Message reply = service.findById(replyId);
    if (reply == null || reply.getParentId() == null || !reply.getParentId().equals(parentId)) {
      return ResponseEntity.notFound().build();
    }
    if (!isOwner(reply, userId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    service.deleteMessage(replyId);
    return ResponseEntity.noContent().build();
  }

  private UUID extractUserId(Jwt jwt) {
    if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private boolean isOwner(Message message, UUID userId) {
    return message.getUserId() != null && message.getUserId().equals(userId);
  }

  private MessageResponse toResponse(Message message) {
    List<MessageResponse> replyResponses = null;
    if (message.getReplies() != null) {
      replyResponses = message.getReplies().stream().map(this::toResponse).toList();
    }

    return new MessageResponse(
        message.getId(),
        message.getContent(),
        message.getCreatedAt(),
        message.getReadingId(),
        message.getUserId(),
        message.getParentId(),
        replyResponses);
  }
}
