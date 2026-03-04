package id.ac.ui.cs.advprog.beforum.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;

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
