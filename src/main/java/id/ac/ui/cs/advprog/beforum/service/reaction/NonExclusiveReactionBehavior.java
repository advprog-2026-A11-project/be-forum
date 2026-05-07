package id.ac.ui.cs.advprog.beforum.service.reaction;

import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.repository.ReactionRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NonExclusiveReactionBehavior implements ReactionBehavior {

  @Override
  public boolean beforeAdd(UUID messageId, UUID userId, ReactionType reactionType,
                           ReactionRepository reactionRepository) throws IllegalStateException {
    // Check if user already has this exact reaction
    var existingReaction = reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, reactionType);

    if (existingReaction.isPresent()) {
      // Toggle: Remove the reaction
      reactionRepository.delete(existingReaction.get());
      return false;  // Don't add a new reaction
    }

    return true;  // Add the new reaction
  }
}
