package id.ac.ui.cs.advprog.beforum.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.service.MessageService;

@RestController
@RequestMapping({"/messages", "/api/messages"})
public class MessageController {

  private final MessageService service;

  public MessageController(MessageService service) {
    this.service = service;
  }

  public static record CreateMessageRequest(String content) {}

  @PostMapping
  public ResponseEntity<Message> create(@RequestBody CreateMessageRequest req) {
    Message created = service.createMessage(req.content());
    return ResponseEntity.ok(created);
  }

  @GetMapping
  public ResponseEntity<List<Message>> list() {
    return ResponseEntity.ok(service.listMessages());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Message> getById(@PathVariable UUID id) {
    Message message = service.findByIdWithReplies(id);
    if (message == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(message);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Message> update(@PathVariable UUID id, @RequestBody CreateMessageRequest req) {
    Message updated = service.updateMessage(id, req.content());
    if (updated == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    Message found = service.findById(id);
    if (found == null) return ResponseEntity.notFound().build();
    service.deleteMessage(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{parentId}/replies")
  public ResponseEntity<Message> createReply(
      @PathVariable UUID parentId,
      @RequestBody CreateMessageRequest req) {
    Message reply = service.createReply(parentId, req.content());
    if (reply == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(reply);
  }

  @GetMapping("/{parentId}/replies")
  public ResponseEntity<List<Message>> getReplies(@PathVariable UUID parentId) {
    Message parent = service.findById(parentId);
    if (parent == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(service.getReplies(parentId));
  }

  @PutMapping("/{parentId}/replies/{replyId}")
  public ResponseEntity<Message> updateReply(
      @PathVariable UUID parentId,
      @PathVariable UUID replyId,
      @RequestBody CreateMessageRequest req) {
    Message reply = service.findById(replyId);
    if (reply == null || reply.getParentId() == null || !reply.getParentId().equals(parentId)) {
      return ResponseEntity.notFound().build();
    }
    Message updated = service.updateMessage(replyId, req.content());
    if (updated == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{parentId}/replies/{replyId}")
  public ResponseEntity<Void> deleteReply(
      @PathVariable UUID parentId,
      @PathVariable UUID replyId) {
    Message reply = service.findById(replyId);
    if (reply == null || reply.getParentId() == null || !reply.getParentId().equals(parentId)) {
      return ResponseEntity.notFound().build();
    }
    service.deleteMessage(replyId);
    return ResponseEntity.noContent().build();
  }
}
