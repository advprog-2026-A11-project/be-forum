package id.ac.ui.cs.advprog.beforum.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.repository.ReactionRepository;
import id.ac.ui.cs.advprog.beforum.service.reaction.NonExclusiveReactionBehavior;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NonExclusiveReactionBehaviorTest {

  @Test
  void beforeAddShouldToggleOffWhenExistingReactionPresent() {
    ReactionRepository repository = mock(ReactionRepository.class);
    NonExclusiveReactionBehavior behavior = new NonExclusiveReactionBehavior();
    UUID messageId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Reaction existing = new Reaction();

    when(repository.findByMessageIdAndUserIdAndReactionType(messageId, userId, ReactionType.FIRE))
        .thenReturn(Optional.of(existing));

    boolean shouldAdd = behavior.beforeAdd(messageId, userId, ReactionType.FIRE, repository);

    assertFalse(shouldAdd);
    verify(repository).delete(existing);
  }

  @Test
  void beforeAddShouldAllowWhenNoExistingReaction() {
    ReactionRepository repository = mock(ReactionRepository.class);
    NonExclusiveReactionBehavior behavior = new NonExclusiveReactionBehavior();
    UUID messageId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(repository.findByMessageIdAndUserIdAndReactionType(messageId, userId, ReactionType.FIRE))
        .thenReturn(Optional.empty());

    boolean shouldAdd = behavior.beforeAdd(messageId, userId, ReactionType.FIRE, repository);

    assertTrue(shouldAdd);
  }
}
