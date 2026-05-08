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
    reaction.setUserId(UUID.randomUUID());

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
    reaction.setUserId(UUID.randomUUID());
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
    UUID userId = UUID.randomUUID();
    reaction.setUserId(userId);

    assertEquals(userId, reaction.getUserId());
  }

  @Test
  void allArgsConstructorShouldWork() {
    UUID id = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.now();
    Message message = new Message();

    UUID userId = UUID.randomUUID();
    Reaction reaction = new Reaction(id, ReactionType.ROCKET, userId, createdAt, message);

    assertEquals(id, reaction.getId());
    assertEquals(ReactionType.ROCKET, reaction.getReactionType());
    assertEquals(userId, reaction.getUserId());
    assertEquals(createdAt, reaction.getCreatedAt());
    assertEquals(message, reaction.getMessage());
  }
}
