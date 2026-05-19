package id.ac.ui.cs.advprog.beforum.controller.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageAuthorizationServiceTest {

  private final MessageAuthorizationService service = new MessageAuthorizationService();

  @Test
  void isOwnerShouldReturnFalseWhenMessageIsNull() {
    assertFalse(service.isOwner(null, UUID.randomUUID()));
  }

  @Test
  void isOwnerShouldReturnFalseWhenOwnerIsNull() {
    Message message = new Message();
    message.setUserId(null);
    assertFalse(service.isOwner(message, UUID.randomUUID()));
  }

  @Test
  void isOwnerShouldReturnFalseWhenDifferentUser() {
    Message message = new Message();
    message.setUserId(UUID.randomUUID());
    assertFalse(service.isOwner(message, UUID.randomUUID()));
  }

  @Test
  void isOwnerShouldReturnTrueWhenSameUser() {
    UUID userId = UUID.randomUUID();
    Message message = new Message();
    message.setUserId(userId);
    assertTrue(service.isOwner(message, userId));
  }

  @Test
  void isReplyOfParentShouldReturnFalseWhenReplyIsNull() {
    assertFalse(service.isReplyOfParent(null, UUID.randomUUID()));
  }

  @Test
  void isReplyOfParentShouldReturnFalseWhenParentIdIsNull() {
    Message reply = new Message();
    reply.setParent(null);
    assertFalse(service.isReplyOfParent(reply, UUID.randomUUID()));
  }

  @Test
  void isReplyOfParentShouldReturnFalseWhenParentDoesNotMatch() {
    Message parent = new Message();
    parent.setId(UUID.randomUUID());
    Message reply = new Message();
    reply.setParent(parent);
    assertFalse(service.isReplyOfParent(reply, UUID.randomUUID()));
  }

  @Test
  void isReplyOfParentShouldReturnTrueWhenParentMatches() {
    UUID parentId = UUID.randomUUID();
    Message parent = new Message();
    parent.setId(parentId);
    Message reply = new Message();
    reply.setParent(parent);
    assertTrue(service.isReplyOfParent(reply, parentId));
  }
}
