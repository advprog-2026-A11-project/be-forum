package id.ac.ui.cs.advprog.beforum.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReactionTest {

  @Test
  void reactionShouldHaveUuid() {
    Reaction reaction = new Reaction();
    assertNotNull(reaction.getId());
  }

  @Test
  void reactionShouldHaveCreatedAtTimestamp() {
    OffsetDateTime before = OffsetDateTime.now();
    Reaction reaction = new Reaction();
    OffsetDateTime after = OffsetDateTime.now();

    assertNotNull(reaction.getCreatedAt());
    assertTrue(reaction.getCreatedAt().isAfter(before.minusSeconds(1)));
    assertTrue(reaction.getCreatedAt().isBefore(after.plusSeconds(1)));
  }

  @Test
  void getMessageIdShouldReturnNullWhenNoMessage() {
    Reaction reaction = new Reaction();
    reaction.setId(UUID.randomUUID());
    reaction.setReactionType(ReactionType.UPVOTE);
    reaction.setUserId("user123");

    assertNull(reaction.getMessageId());
  }

  @Test
  void getMessageIdShouldReturnMessageIdWhenMessageExists() {
    Message message = new Message();
    UUID messageId = UUID.randomUUID();
    message.setId(messageId);
    message.setContent("Test content");

    Reaction reaction = new Reaction();
    reaction.setId(UUID.randomUUID());
    reaction.setReactionType(ReactionType.UPVOTE);
    reaction.setUserId("user123");
    reaction.setMessage(message);

    assertEquals(messageId, reaction.getMessageId());
  }

  @Test
  void reactionTypeShouldBeSetCorrectly() {
    Reaction reaction = new Reaction();
    reaction.setReactionType(ReactionType.FIRE);

    assertEquals(ReactionType.FIRE, reaction.getReactionType());
  }

  @Test
  void userIdShouldBeSetCorrectly() {
    Reaction reaction = new Reaction();
    reaction.setUserId("user456");

    assertEquals("user456", reaction.getUserId());
  }

  @Test
  void allArgsConstructorShouldWork() {
    UUID id = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.now();
    Message message = new Message();

    Reaction reaction = new Reaction(id, ReactionType.ROCKET, "user789", createdAt, message);

    assertEquals(id, reaction.getId());
    assertEquals(ReactionType.ROCKET, reaction.getReactionType());
    assertEquals("user789", reaction.getUserId());
    assertEquals(createdAt, reaction.getCreatedAt());
    assertEquals(message, reaction.getMessage());
  }
}
