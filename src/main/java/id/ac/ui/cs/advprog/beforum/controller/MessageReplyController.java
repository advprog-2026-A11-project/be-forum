package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationService;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageResponseMapper;
import id.ac.ui.cs.advprog.beforum.controller.support.UseCaseRequestHandler;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages/{parentId}/replies")
public class MessageReplyController {

  private final MessageService service;
  private final UseCaseRequestHandler requestHandler;
  private final MessageAuthorizationService authorizationService;
  private final MessageResponseMapper responseMapper;

  public MessageReplyController(
      MessageService service,
      UseCaseRequestHandler requestHandler,
      MessageAuthorizationService authorizationService,
      MessageResponseMapper responseMapper) {
    this.service = service;
    this.requestHandler = requestHandler;
    this.authorizationService = authorizationService;
    this.responseMapper = responseMapper;
  }

  @PostMapping
  public ResponseEntity<MessageResponse> createReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @RequestBody CreateMessageRequest req) {
    return requestHandler.withAuthenticatedUuid(jwt, userId -> {
      Message reply = service.createReply(parentId, req.content(), userId);
      if (reply == null) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(responseMapper.toResponse(reply));
    });
  }

  @GetMapping
  public ResponseEntity<List<MessageResponse>> getReplies(@PathVariable UUID parentId) {
    Message parent = service.findById(parentId);
    if (parent == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(responseMapper.toResponses(service.getReplies(parentId)));
  }

  @PutMapping("/{replyId}")
  public ResponseEntity<MessageResponse> updateReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @PathVariable UUID replyId,
      @RequestBody CreateMessageRequest req) {
    return requestHandler.withAuthenticatedUuid(jwt, userId -> {
      Message reply = service.findById(replyId);
      return requestHandler.require(
          authorizationService.isReplyOfParent(reply, parentId),
          HttpStatus.NOT_FOUND,
          () -> requestHandler.require(
              authorizationService.isOwner(reply, userId),
              HttpStatus.FORBIDDEN,
              () -> {
                Message updated = service.updateMessage(replyId, req.content());
                if (updated == null) {
                  return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok(responseMapper.toResponse(updated));
              }));
    });
  }

  @DeleteMapping("/{replyId}")
  public ResponseEntity<Void> deleteReply(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID parentId,
      @PathVariable UUID replyId) {
    return requestHandler.withAuthenticatedUuid(jwt, userId -> {
      Message reply = service.findById(replyId);
      return requestHandler.require(
          authorizationService.isReplyOfParent(reply, parentId),
          HttpStatus.NOT_FOUND,
          () -> requestHandler.require(
              authorizationService.isOwner(reply, userId),
              HttpStatus.FORBIDDEN,
              () -> {
                service.deleteMessage(replyId);
                return ResponseEntity.noContent().build();
              }));
    });
  }
}
