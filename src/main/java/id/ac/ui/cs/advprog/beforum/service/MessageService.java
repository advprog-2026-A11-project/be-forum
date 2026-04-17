package id.ac.ui.cs.advprog.beforum.service;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

  private final MessageRepository repository;

  public MessageService(MessageRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Message createMessage(String content, String readingId, UUID userId) {
    if (readingId == null || readingId.isBlank()) {
      throw new IllegalArgumentException("readingId is required for thread creation");
    }

    Message message = new Message();
    message.setContent(content);
    message.setReadingId(readingId.trim());
    message.setUserId(userId);
    return repository.save(message);
  }

  @Transactional(readOnly = true)
  public List<Message> listMessages(String readingId) {
    List<Message> messages;
    if (readingId == null || readingId.isBlank()) {
      messages = repository.findTopLevelOrderByCreatedAtDesc();
    } else {
      messages = repository.findTopLevelByReadingIdOrderByCreatedAtDesc(readingId.trim());
    }
    messages.forEach(this::loadRepliesRecursively);
    return messages;
  }

  @Transactional(readOnly = true)
  public Message findById(UUID id) {
    return repository.findById(id).orElse(null);
  }

  @Transactional
  public Message updateMessage(UUID id, String content) {
    return repository.findById(id)
        .map(m -> {
          m.setContent(content);
          return repository.save(m);
        })
        .orElse(null);
  }

  @Transactional
  public void deleteMessage(UUID id) {
    repository.deleteById(id);
  }

  @Transactional
  public Message createReply(UUID parentId, String content, UUID userId) {
    return repository.findById(parentId)
        .map(parent -> {
          Message reply = new Message();
          reply.setContent(content);
          reply.setReadingId(parent.getReadingId());
          reply.setUserId(userId);
          reply.setParent(parent);
          return repository.save(reply);
        })
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public List<Message> getReplies(UUID parentId) {
    return repository.findByParentIdOrderByCreatedAtAsc(parentId);
  }

  @Transactional(readOnly = true)
  public Message findByIdWithReplies(UUID id) {
    return repository.findById(id)
        .map(message -> {
          loadRepliesRecursively(message);
          return message;
        })
        .orElse(null);
  }

  private void loadRepliesRecursively(Message message) {
    if (message.getReplies() != null) {
      message.getReplies().size();
      for (Message reply : message.getReplies()) {
        loadRepliesRecursively(reply);
      }
    }
  }
}
