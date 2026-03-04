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
  public Message createMessage(String content) {
    Message message = new Message();
    message.setContent(content);
    return repository.save(message);
  }

  @Transactional(readOnly = true)
  public List<Message> listMessages() {
    List<Message> messages = repository.findAll();
    messages.forEach(this::loadRepliesRecursively);
    return messages;
  }

  @Transactional(readOnly = true)
  public List<Message> listTopLevelMessages() {
    List<Message> messages = repository.findByParentIsNull();
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

  // ================== Reply CRUD Operations ==================

  /**
   * Create a reply to an existing message.
   * The parent can be any message (including another reply), enabling nested replies.
   * 
   * @param parentId the ID of the parent message
   * @param content the content of the reply
   * @return the created reply message, or null if parent not found
   */
  @Transactional
  public Message createReply(UUID parentId, String content) {
    return repository.findById(parentId)
        .map(parent -> {
          Message reply = new Message();
          reply.setContent(content);
          reply.setParent(parent);
          return repository.save(reply);
        })
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public List<Message> getReplies(UUID parentId) {
    return repository.findByParent_IdOrderByCreatedAtAsc(parentId);
  }

  /**
   * Update a reply's content.
   * This is the same as updateMessage since replies are also messages.
   * 
   * @param replyId the ID of the reply to update
   * @param content the new content
   * @return the updated reply, or null if not found
   */
  @Transactional
  public Message updateReply(UUID replyId, String content) {
    return updateMessage(replyId, content);
  }

  /**
   * Delete a reply and all its nested replies (cascade).
   * This is the same as deleteMessage since replies are also messages.
   * 
   * @param replyId the ID of the reply to delete
   */
  @Transactional
  public void deleteReply(UUID replyId) {
    deleteMessage(replyId);
  }

  /**
   * Get a message with its full reply tree.
   * 
   * @param id the message ID
   * @return the message with all nested replies loaded
   */
  @Transactional(readOnly = true)
  public Message findByIdWithReplies(UUID id) {
    return repository.findById(id)
        .map(message -> {
          // Force loading of replies (they're lazy loaded)
          loadRepliesRecursively(message);
          return message;
        })
        .orElse(null);
  }

  /**
   * Recursively load all nested replies.
   */
  private void loadRepliesRecursively(Message message) {
    if (message.getReplies() != null) {
      message.getReplies().size(); // Force lazy loading
      for (Message reply : message.getReplies()) {
        loadRepliesRecursively(reply);
      }
    }
  }
}
