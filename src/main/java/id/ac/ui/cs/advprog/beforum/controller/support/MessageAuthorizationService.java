package id.ac.ui.cs.advprog.beforum.controller.support;

import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MessageAuthorizationService {

  public boolean isOwner(Message message, UUID userId) {
    return message != null && message.getUserId() != null && message.getUserId().equals(userId);
  }

  public boolean isReplyOfParent(Message reply, UUID parentId) {
    return reply != null
        && reply.getParentId() != null
        && reply.getParentId().equals(parentId);
  }
}
