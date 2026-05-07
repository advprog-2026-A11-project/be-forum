package id.ac.ui.cs.advprog.beforum.service.reaction;

import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.repository.ReactionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ExclusiveReactionBehavior implements ReactionBehavior {

  private final List<Set<ReactionType>> exclusiveGroups;

  public ExclusiveReactionBehavior(List<Set<ReactionType>> exclusiveGroups) {
    this.exclusiveGroups = exclusiveGroups;
  }

  @Override
  public boolean beforeAdd(UUID messageId, UUID userId, ReactionType reactionType,
                           ReactionRepository reactionRepository) throws IllegalStateException {
    // Find which exclusive group this reaction type belongs to
    Set<ReactionType> conflictGroup = exclusiveGroups.stream()
        .filter(group -> group.contains(reactionType))
        .findFirst()
        .orElse(Set.of());

    // Delete all other types in THIS group only
    for (ReactionType conflictType : conflictGroup) {
      if (conflictType != reactionType) {
        reactionRepository.findByMessageIdAndUserIdAndReactionType(
                messageId, userId, conflictType)
            .ifPresent(reactionRepository::delete);
      }
    }

    // Check for duplicate (toggle)
    var existing = reactionRepository.findByMessageIdAndUserIdAndReactionType(
        messageId, userId, reactionType);

    if (existing.isPresent()) {
      reactionRepository.delete(existing.get());
      return false;
    }

    return true;
  }

  public boolean containsType(ReactionType reactionType) {
    return exclusiveGroups.stream()
        .anyMatch(group -> group.contains(reactionType));
  }
}
