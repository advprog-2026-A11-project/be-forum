package id.ac.ui.cs.advprog.beforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;
import id.ac.ui.cs.advprog.beforum.repository.ReactionRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

  @Mock
  private ReactionRepository reactionRepository;

  @Mock
  private MessageRepository messageRepository;

  @InjectMocks
  private ReactionService service;

  private Message message;
  private Reaction reaction;
  private UUID messageId;
  private UUID reactionId;
  private String userId;

  @BeforeEach
  void setUp() {
    messageId = UUID.randomUUID();
    reactionId = UUID.randomUUID();
    userId = "user123";

    message = new Message();
    message.setId(messageId);
    message.setContent("Test message content");
    message.setCreatedAt(OffsetDateTime.now());

    reaction = new Reaction();
    reaction.setId(reactionId);
    reaction.setReactionType(ReactionType.UPVOTE);
    reaction.setUserId(userId);
    reaction.setMessage(message);
    reaction.setCreatedAt(OffsetDateTime.now());
  }

  @Test
  void addReactionShouldCreateReaction() {
    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.FIRE
    )).thenReturn(Optional.empty());
    when(reactionRepository.save(any(Reaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reaction created = service.addReaction(messageId, userId, ReactionType.FIRE);

    assertNotNull(created);
    assertEquals(ReactionType.FIRE, created.getReactionType());
    assertEquals(userId, created.getUserId());
    assertEquals(message, created.getMessage());
    verify(reactionRepository).save(any(Reaction.class));
  }

  @Test
  void addReactionShouldReturnNullWhenMessageNotFound() {
    UUID nonExistentMessageId = UUID.randomUUID();
    when(messageRepository.findById(nonExistentMessageId)).thenReturn(Optional.empty());

    Reaction created = service.addReaction(nonExistentMessageId, userId, ReactionType.UPVOTE);

    assertNull(created);
    verify(reactionRepository, never()).save(any(Reaction.class));
  }

  @Test
  void addReactionShouldReturnExistingReactionWhenDuplicate() {
    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.UPVOTE
    )).thenReturn(Optional.of(reaction));

    Reaction result = service.addReaction(messageId, userId, ReactionType.UPVOTE);

    assertEquals(reaction, result);
    verify(reactionRepository, never()).save(any(Reaction.class));
  }

  @Test
  void addUpvoteShouldRemoveExistingDownvote() {
    Reaction existingDownvote = new Reaction();
    existingDownvote.setId(UUID.randomUUID());
    existingDownvote.setReactionType(ReactionType.DOWNVOTE);
    existingDownvote.setUserId(userId);
    existingDownvote.setMessage(message);

    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.UPVOTE
    )).thenReturn(Optional.empty());
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.DOWNVOTE
    )).thenReturn(Optional.of(existingDownvote));
    when(reactionRepository.save(any(Reaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reaction result = service.addReaction(messageId, userId, ReactionType.UPVOTE);

    assertNotNull(result);
    assertEquals(ReactionType.UPVOTE, result.getReactionType());
    verify(reactionRepository).delete(existingDownvote);
    verify(reactionRepository).save(any(Reaction.class));
  }

  @Test
  void addDownvoteShouldRemoveExistingUpvote() {
    Reaction existingUpvote = new Reaction();
    existingUpvote.setId(UUID.randomUUID());
    existingUpvote.setReactionType(ReactionType.UPVOTE);
    existingUpvote.setUserId(userId);
    existingUpvote.setMessage(message);

    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.DOWNVOTE
    )).thenReturn(Optional.empty());
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.UPVOTE
    )).thenReturn(Optional.of(existingUpvote));
    when(reactionRepository.save(any(Reaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reaction result = service.addReaction(messageId, userId, ReactionType.DOWNVOTE);

    assertNotNull(result);
    assertEquals(ReactionType.DOWNVOTE, result.getReactionType());
    verify(reactionRepository).delete(existingUpvote);
    verify(reactionRepository).save(any(Reaction.class));
  }

  @Test
  void removeReactionShouldDeleteExistingReaction() {
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.UPVOTE
    )).thenReturn(Optional.of(reaction));

    boolean removed = service.removeReaction(messageId, userId, ReactionType.UPVOTE);

    assertTrue(removed);
    verify(reactionRepository).delete(reaction);
  }

  @Test
  void removeReactionShouldReturnFalseWhenReactionNotFound() {
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.UPVOTE
    )).thenReturn(Optional.empty());

    boolean removed = service.removeReaction(messageId, userId, ReactionType.UPVOTE);

    assertFalse(removed);
    verify(reactionRepository, never()).delete(any(Reaction.class));
  }

  @Test
  void getReactionsByMessageIdShouldReturnReactions() {
    Reaction reaction2 = new Reaction();
    reaction2.setId(UUID.randomUUID());
    reaction2.setReactionType(ReactionType.FIRE);
    reaction2.setUserId("user456");
    reaction2.setMessage(message);

    List<Reaction> reactions = Arrays.asList(reaction, reaction2);
    when(reactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId)).thenReturn(reactions);

    List<Reaction> result = service.getReactionsByMessageId(messageId);

    assertEquals(2, result.size());
    assertEquals(reaction, result.get(0));
    assertEquals(reaction2, result.get(1));
    verify(reactionRepository).findByMessageIdOrderByCreatedAtAsc(messageId);
  }

  @Test
  void getReactionsByMessageIdShouldReturnEmptyListWhenNoReactions() {
    when(reactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId)).thenReturn(List.of());

    List<Reaction> result = service.getReactionsByMessageId(messageId);

    assertTrue(result.isEmpty());
  }

  @Test
  void getUserReactionsOnMessageShouldReturnUserReactions() {
    List<Reaction> reactions = Arrays.asList(reaction);
    when(reactionRepository.findByMessageIdAndUserId(messageId, userId)).thenReturn(reactions);

    List<Reaction> result = service.getUserReactionsOnMessage(messageId, userId);

    assertEquals(1, result.size());
    assertEquals(reaction, result.get(0));
    verify(reactionRepository).findByMessageIdAndUserId(messageId, userId);
  }

  @Test
  void getReactionCountsByMessageIdShouldReturnCounts() {
    when(reactionRepository.countByMessageIdAndReactionType(
        messageId, ReactionType.UPVOTE
    )).thenReturn(5L);
    when(reactionRepository.countByMessageIdAndReactionType(
        messageId, ReactionType.DOWNVOTE
    )).thenReturn(2L);
    when(reactionRepository.countByMessageIdAndReactionType(
        messageId, ReactionType.FIRE
    )).thenReturn(3L);
    when(reactionRepository.countByMessageIdAndReactionType(
        messageId, ReactionType.ROCKET
    )).thenReturn(0L);
    when(reactionRepository.countByMessageIdAndReactionType(
        messageId, ReactionType.LAUGH
    )).thenReturn(1L);
    when(reactionRepository.countByMessageIdAndReactionType(
        messageId, ReactionType.PARTY
    )).thenReturn(0L);
    when(reactionRepository.countByMessageIdAndReactionType(
        messageId, ReactionType.THINKING
    )).thenReturn(0L);

    Map<ReactionType, Long> counts = service.getReactionCountsByMessageId(messageId);

    assertEquals(7, counts.size());
    assertEquals(5L, counts.get(ReactionType.UPVOTE));
    assertEquals(2L, counts.get(ReactionType.DOWNVOTE));
    assertEquals(3L, counts.get(ReactionType.FIRE));
    assertEquals(0L, counts.get(ReactionType.ROCKET));
    assertEquals(1L, counts.get(ReactionType.LAUGH));
    assertEquals(0L, counts.get(ReactionType.PARTY));
    assertEquals(0L, counts.get(ReactionType.THINKING));
  }

  @Test
  void findByIdShouldReturnReaction() {
    when(reactionRepository.findById(reactionId)).thenReturn(Optional.of(reaction));

    Reaction result = service.findById(reactionId);

    assertEquals(reaction, result);
    verify(reactionRepository).findById(reactionId);
  }

  @Test
  void findByIdShouldReturnNullWhenNotFound() {
    UUID nonExistentId = UUID.randomUUID();
    when(reactionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    Reaction result = service.findById(nonExistentId);

    assertNull(result);
  }

  @Test
  void addEmojiReactionShouldNotAffectVotes() {
    when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
    when(reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, ReactionType.FIRE
    )).thenReturn(Optional.empty());
    when(reactionRepository.save(any(Reaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reaction result = service.addReaction(messageId, userId, ReactionType.FIRE);

    assertNotNull(result);
    assertEquals(ReactionType.FIRE, result.getReactionType());
    verify(reactionRepository, never())
        .findByMessageIdAndUserIdAndReactionType(
            messageId, userId, ReactionType.UPVOTE
        );
    verify(reactionRepository, never())
        .findByMessageIdAndUserIdAndReactionType(
            messageId, userId, ReactionType.DOWNVOTE
        );
    verify(reactionRepository).save(any(Reaction.class));
  }
}
