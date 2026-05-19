package id.ac.ui.cs.advprog.beforum.service;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;
import id.ac.ui.cs.advprog.beforum.repository.ReactionRepository;
import id.ac.ui.cs.advprog.beforum.service.reaction.ReactionBehaviorRegistry;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactionService {

  private final ReactionRepository reactionRepository;
  private final MessageRepository messageRepository;
  private final ReactionBehaviorRegistry behaviorRegistry;

  public ReactionService(ReactionRepository reactionRepository,
                         MessageRepository messageRepository,
                         ReactionBehaviorRegistry behaviorRegistry) {
    this.reactionRepository = reactionRepository;
    this.messageRepository = messageRepository;
    this.behaviorRegistry = behaviorRegistry;
  }

  @Transactional
  public Reaction addReaction(UUID messageId, UUID userId, ReactionType reactionType) {
    AddReactionResult result = addReactionWithOutcome(messageId, userId, reactionType);
    return result.outcome() == AddReactionOutcome.ADDED ? result.reaction() : null;
  }

  @Transactional
  public AddReactionResult addReactionWithOutcome(
      UUID messageId, UUID userId, ReactionType reactionType) {
    Optional<Message> messageOpt = messageRepository.findById(messageId);
    if (messageOpt.isEmpty()) {
      return new AddReactionResult(AddReactionOutcome.MESSAGE_NOT_FOUND, null);
    }

    // Use strategy pattern to handle type-specific behavior
    var behavior = behaviorRegistry.getBehavior(reactionType);
    boolean shouldAdd = behavior.beforeAdd(messageId, userId, reactionType, reactionRepository);

    // If behavior returns false, reaction was toggled off (deleted)
    if (!shouldAdd) {
      return new AddReactionResult(AddReactionOutcome.TOGGLED_OFF, null);
    }

    // Add the new reaction
    Reaction reaction = new Reaction();
    reaction.setReactionType(reactionType);
    reaction.setUserId(userId);
    reaction.setMessage(messageOpt.get());
    return new AddReactionResult(
        AddReactionOutcome.ADDED,
        reactionRepository.save(reaction));
  }

  @Transactional
  public boolean removeReaction(UUID messageId,
                                UUID userId,
                                ReactionType reactionType) {
    Optional<Reaction> existingReaction =
        reactionRepository.findByMessageIdAndUserIdAndReactionType(
            messageId,
            userId,
            reactionType);
    if (existingReaction.isPresent()) {
      reactionRepository.delete(existingReaction.get());
      return true;
    }
    return false;
  }

  @Transactional(readOnly = true)
  public List<Reaction> getReactionsByMessageId(UUID messageId) {
    return reactionRepository.findByMessageIdOrderByCreatedAtAsc(messageId);
  }

  @Transactional(readOnly = true)
  public List<Reaction> getUserReactionsOnMessage(UUID messageId, UUID userId) {
    return reactionRepository.findByMessageIdAndUserId(
        messageId,
        userId
    );
  }

  @Transactional(readOnly = true)
  public Map<ReactionType, Long> getReactionCountsByMessageId(UUID messageId) {
    Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
    for (ReactionType type : ReactionType.values()) {
      counts.put(
          type,
          reactionRepository.countByMessageIdAndReactionType(messageId, type)
      );
    }
    return counts;
  }

  public enum AddReactionOutcome {
    ADDED,
    TOGGLED_OFF,
    MESSAGE_NOT_FOUND
  }

  public record AddReactionResult(AddReactionOutcome outcome, Reaction reaction) {
  }

  @Transactional(readOnly = true)
  public Reaction findById(UUID id) {
    return reactionRepository.findById(id).orElse(null);
  }
}
