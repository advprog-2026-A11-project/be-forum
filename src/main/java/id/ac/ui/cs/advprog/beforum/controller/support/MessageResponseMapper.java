package id.ac.ui.cs.advprog.beforum.controller.support;

import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.dto.ReactionResponse;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MessageResponseMapper {

  public List<MessageResponse> toResponses(List<Message> messages) {
    return messages.stream().map(this::toResponse).toList();
  }

  public List<MessageResponse> toResponsesShallow(
      List<Message> messages, Map<java.util.UUID, Long> replyCounts) {
    return messages.stream().map(message -> toResponseShallow(message, replyCounts)).toList();
  }

  public MessageResponse toResponse(Message message) {
    List<MessageResponse> replyResponses = null;
    if (message.getReplies() != null) {
      replyResponses = message.getReplies().stream().map(this::toResponse).toList();
    }

    List<ReactionResponse> reactionResponses = mapReactions(message.getReactions());
    Map<ReactionType, Long> reactionCounts = buildReactionCounts(reactionResponses);

    return new MessageResponse(
        message.getId(),
        message.getContent(),
        message.getCreatedAt(),
        message.getReadingId(),
        message.getUserId(),
        message.getParentId(),
        message.getReplies() != null ? (long) message.getReplies().size() : 0L,
        replyResponses,
        reactionResponses,
        reactionCounts);
  }

  public MessageResponse toResponseShallow(
      Message message, Map<java.util.UUID, Long> replyCounts) {
    List<ReactionResponse> reactionResponses = mapReactions(message.getReactions());
    Map<ReactionType, Long> reactionCounts = buildReactionCounts(reactionResponses);

    return new MessageResponse(
        message.getId(),
        message.getContent(),
        message.getCreatedAt(),
        message.getReadingId(),
        message.getUserId(),
        message.getParentId(),
        replyCounts.getOrDefault(message.getId(), 0L),
        null,
        reactionResponses,
        reactionCounts);
  }

  private List<ReactionResponse> mapReactions(List<Reaction> reactions) {
    if (reactions == null) {
      return List.of();
    }
    return reactions.stream()
        .map(
            reaction ->
                new ReactionResponse(
                    reaction.getId(),
                    reaction.getReactionType(),
                    reaction.getUserId(),
                    reaction.getCreatedAt(),
                    reaction.getMessageId()))
        .toList();
  }

  private Map<ReactionType, Long> buildReactionCounts(List<ReactionResponse> reactions) {
    Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
    for (ReactionType type : ReactionType.values()) {
      counts.put(type, 0L);
    }
    for (ReactionResponse reaction : reactions) {
      counts.computeIfPresent(reaction.reactionType(), (key, value) -> value + 1L);
    }
    return counts;
  }
}
