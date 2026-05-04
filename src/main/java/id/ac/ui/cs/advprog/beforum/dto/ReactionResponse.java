package id.ac.ui.cs.advprog.beforum.dto;

import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReactionResponse(
    UUID id,
    ReactionType reactionType,
    UUID userId,
    OffsetDateTime createdAt,
    UUID messageId) {
}
