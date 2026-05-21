package id.ac.ui.cs.advprog.beforum.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageTest {

  @Test
  void getParentIdShouldReturnNullWhenNoParent() {
    Message message = new Message();
    message.setId(UUID.randomUUID());
    message.setContent("Test content");

    assertNull(message.getParentId());
  }

  @Test
  void getParentIdShouldReturnParentIdWhenParentExists() {
    Message parent = new Message();
    UUID parentId = UUID.randomUUID();
    parent.setId(parentId);
    parent.setContent("Parent content");

    Message reply = new Message();
    reply.setId(UUID.randomUUID());
    reply.setContent("Reply content");
    reply.setParent(parent);

    assertEquals(parentId, reply.getParentId());
  }

  @Test
  void repliesShouldBeEmptyByDefault() {
    Message message = new Message();

    assertNotNull(message.getReplies());
    assertTrue(message.getReplies().isEmpty());
  }

  @Test
  void addReplyShouldAddToRepliesList() {
    Message parent = new Message();
    parent.setId(UUID.randomUUID());
    parent.setContent("Parent content");

    Message reply = new Message();
    reply.setId(UUID.randomUUID());
    reply.setContent("Reply content");
    reply.setParent(parent);
    parent.getReplies().add(reply);

    assertEquals(1, parent.getReplies().size());
    assertEquals(reply, parent.getReplies().get(0));
  }

  @Test
  void nestedRepliesShouldWork() {
    Message parent = new Message();
    parent.setId(UUID.randomUUID());
    parent.setContent("Parent content");

    Message reply1 = new Message();
    reply1.setId(UUID.randomUUID());
    reply1.setContent("First level reply");
    reply1.setParent(parent);
    parent.getReplies().add(reply1);

    Message reply2 = new Message();
    reply2.setId(UUID.randomUUID());
    reply2.setContent("Second level reply");
    reply2.setParent(reply1);
    reply1.getReplies().add(reply2);

    assertEquals(1, parent.getReplies().size());
    assertEquals(1, reply1.getReplies().size());
    assertEquals(parent.getId(), reply1.getParentId());
    assertEquals(reply1.getId(), reply2.getParentId());
  }

  @Test
  void messageShouldHaveCreatedAtTimestamp() {
    OffsetDateTime before = OffsetDateTime.now();
    Message message = new Message();
    OffsetDateTime after = OffsetDateTime.now();

    assertNotNull(message.getCreatedAt());
    assertTrue(message.getCreatedAt().isAfter(before.minusSeconds(1)));
    assertTrue(message.getCreatedAt().isBefore(after.plusSeconds(1)));
  }

  @Test
  void messageShouldHaveUuid() {
    Message message = new Message();

    assertNotNull(message.getId());
  }

  @Test
  void allArgsConstructorAndAccessorsShouldPreserveValues() {
    UUID id = UUID.randomUUID();
    Integer version = 2;
    OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
    UUID userId = UUID.randomUUID();

    Message parent = new Message();
    UUID parentId = UUID.randomUUID();
    parent.setId(parentId);

    Message reply = new Message();
    reply.setId(UUID.randomUUID());
    List<Message> replies = new ArrayList<>();
    replies.add(reply);

    Reaction reaction = new Reaction();
    reaction.setId(UUID.randomUUID());
    List<Reaction> reactions = new ArrayList<>();
    reactions.add(reaction);

    Message message = new Message(
        id,
        version,
        "constructed",
        createdAt,
        "reading-xyz",
        userId,
        parent,
        replies,
        reactions);

    assertEquals(id, message.getId());
    assertEquals(version, message.getVersion());
    assertEquals("constructed", message.getContent());
    assertEquals(createdAt, message.getCreatedAt());
    assertEquals("reading-xyz", message.getReadingId());
    assertEquals(userId, message.getUserId());
    assertEquals(parent, message.getParent());
    assertEquals(parentId, message.getParentId());
    assertEquals(replies, message.getReplies());
    assertEquals(reactions, message.getReactions());

    message.setVersion(3);
    message.setReplies(new ArrayList<>());
    message.setReactions(new ArrayList<>());

    assertEquals(3, message.getVersion());
    assertTrue(message.getReplies().isEmpty());
    assertTrue(message.getReactions().isEmpty());
  }
}
