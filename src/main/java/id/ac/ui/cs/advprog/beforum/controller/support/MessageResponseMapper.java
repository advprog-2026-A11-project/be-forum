package id.ac.ui.cs.advprog.beforum.controller.support;

import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MessageResponseMapper {

  public List<MessageResponse> toResponses(List<Message> messages) {
    return messages.stream().map(this::toResponse).toList();
  }

  public MessageResponse toResponse(Message message) {
    List<MessageResponse> replyResponses = null;
    if (message.getReplies() != null) {
      replyResponses = message.getReplies().stream().map(this::toResponse).toList();
    }

    return new MessageResponse(
        message.getId(),
        message.getContent(),
        message.getCreatedAt(),
        message.getReadingId(),
        message.getUserId(),
        message.getParentId(),
        replyResponses);
  }
}
