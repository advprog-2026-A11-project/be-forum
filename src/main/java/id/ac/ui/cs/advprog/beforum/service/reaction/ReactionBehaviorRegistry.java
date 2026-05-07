package id.ac.ui.cs.advprog.beforum.service.reaction;

import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import org.springframework.stereotype.Component;

@Component
public class ReactionBehaviorRegistry {

  private final ExclusiveReactionBehavior exclusiveReactionBehavior;
  private final NonExclusiveReactionBehavior nonExclusiveReactionBehavior;

  public ReactionBehaviorRegistry(
      NonExclusiveReactionBehavior nonExclusiveReactionBehavior) {
    this.nonExclusiveReactionBehavior = nonExclusiveReactionBehavior;

    // Create exclusive reaction behavior with all exclusive groups
    this.exclusiveReactionBehavior =
        new ExclusiveReactionBehavior(java.util.List.of(
            java.util.Set.of(ReactionType.UPVOTE, ReactionType.DOWNVOTE)
        ));
  }

  public ReactionBehavior getBehavior(ReactionType reactionType) {
    return exclusiveReactionBehavior.containsType(reactionType)
        ? exclusiveReactionBehavior
        : nonExclusiveReactionBehavior;
  }
}
