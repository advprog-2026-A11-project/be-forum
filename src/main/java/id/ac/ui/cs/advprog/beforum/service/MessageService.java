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
    return repository.findAll();
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
}
