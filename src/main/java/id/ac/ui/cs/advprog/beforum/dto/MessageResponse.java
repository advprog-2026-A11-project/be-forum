package id.ac.ui.cs.advprog.beforum.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    String content,
    OffsetDateTime createdAt,
    String readingId,
    UUID userId,
    UUID parentId,
    List<MessageResponse> replies) {
}
