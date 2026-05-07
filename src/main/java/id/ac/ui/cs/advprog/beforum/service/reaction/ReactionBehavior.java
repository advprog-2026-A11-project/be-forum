package id.ac.ui.cs.advprog.beforum.service.reaction;

import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.repository.ReactionRepository;
import java.util.UUID;

public interface ReactionBehavior {

  /**
   * Handles reaction logic before adding a reaction.
   * May include:
   * - Toggle behavior (remove if already exists)
   * - Conflict resolution (remove opposite vote)
   * - Custom validation
   *
   * @param messageId          The message ID
   * @param userId             The user ID
   * @param reactionType       The reaction type being added
   * @param reactionRepository Repository for querying/deleting reactions
   * @return true if reaction should be added, false if toggled off (user removed it)
   * @throws IllegalStateException if the operation is invalid
   */
  boolean beforeAdd(UUID messageId, UUID userId, ReactionType reactionType,
                    ReactionRepository reactionRepository) throws IllegalStateException;
}

