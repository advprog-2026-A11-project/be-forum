package id.ac.ui.cs.advprog.beforum.dto;

import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    String content,
    OffsetDateTime createdAt,
    String readingId,
    UUID userId,
    UUID parentId,
    Long replyCount,
    List<MessageResponse> replies,
    List<ReactionResponse> reactions,
    Map<ReactionType, Long> reactionCounts) {
}
