package id.ac.ui.cs.advprog.beforum.service;


import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;
import id.ac.ui.cs.advprog.beforum.repository.ReactionRepository;
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

  public ReactionService(ReactionRepository reactionRepository,
                         MessageRepository messageRepository) {
    this.reactionRepository = reactionRepository;
    this.messageRepository = messageRepository;
  }

  @Transactional
  public Reaction addReaction(UUID messageId, String userId, ReactionType reactionType) {
    Optional<Message> messageOpt = messageRepository.findById(messageId);
    if (messageOpt.isEmpty()) {
      return null;
    }

    // Check if user already has this reaction on this message
    Optional<Reaction> existingReaction =
      reactionRepository.findByMessageIdAndUserIdAndReactionType(
          messageId,
          userId,
          reactionType
      );
    if (existingReaction.isPresent()) {
      return existingReaction.get(); // Reaction already exists
    }

    // For upvote/downvote, remove any existing vote before adding new one
    if (reactionType == ReactionType.UPVOTE || reactionType == ReactionType.DOWNVOTE) {
      ReactionType oppositeVote = reactionType == ReactionType.UPVOTE
        ? ReactionType.DOWNVOTE
        : ReactionType.UPVOTE;
        reactionRepository.findByMessageIdAndUserIdAndReactionType(
          messageId,
          userId,
          oppositeVote
        )
          .ifPresent(reactionRepository::delete);
    }

    Reaction reaction = new Reaction();
    reaction.setReactionType(reactionType);
    reaction.setUserId(userId);
    reaction.setMessage(messageOpt.get());
    return reactionRepository.save(reaction);
  }

  @Transactional
    public boolean removeReaction(UUID messageId,
                  String userId,
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
  public List<Reaction> getUserReactionsOnMessage(UUID messageId, String userId) {
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

  @Transactional(readOnly = true)
  public Reaction findById(UUID id) {
    return reactionRepository.findById(id).orElse(null);
  }
}
