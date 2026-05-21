package id.ac.ui.cs.advprog.beforum.service;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

  private final MessageRepository repository;
  private final ForumMetricsService forumMetricsService;

  public MessageService(MessageRepository repository, ForumMetricsService forumMetricsService) {
    this.repository = repository;
    this.forumMetricsService = forumMetricsService;
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
    forumMetricsService.incrementDbOperation("write", "MessageRepository.save");
    return repository.save(message);
  }

  @Transactional(readOnly = true)
  public List<Message> listMessages(String readingId) {
    if (readingId == null || readingId.isBlank()) {
      forumMetricsService.incrementDbOperation("read", "MessageRepository.findTopLevelOrderByCreatedAtDesc");
      return repository.findTopLevelOrderByCreatedAtDesc();
    }
    forumMetricsService.incrementDbOperation("read", "MessageRepository.findTopLevelByReadingIdOrderByCreatedAtDesc");
    return repository.findTopLevelByReadingIdOrderByCreatedAtDesc(readingId.trim());
  }

  @Transactional(readOnly = true)
  public Message findById(UUID id) {
    forumMetricsService.incrementDbOperation("read", "MessageRepository.findById");
    return repository.findById(id).orElse(null);
  }

  @Transactional
  public Message updateMessage(UUID id, String content) {
    forumMetricsService.incrementDbOperation("read", "MessageRepository.findById");
    return repository.findById(id)
        .map(m -> {
          m.setContent(content);
          forumMetricsService.incrementDbOperation("write", "MessageRepository.save");
          return repository.save(m);
        })
        .orElse(null);
  }

  @Transactional
  public void deleteMessage(UUID id) {
    forumMetricsService.incrementDbOperation("write", "MessageRepository.deleteById");
    repository.deleteById(id);
  }

  @Transactional
  public Message createReply(UUID parentId, String content, UUID userId) {
    forumMetricsService.incrementDbOperation("read", "MessageRepository.findById");
    return repository.findById(parentId)
        .map(parent -> {
          Message reply = new Message();
          reply.setContent(content);
          reply.setReadingId(parent.getReadingId());
          reply.setUserId(userId);
          reply.setParent(parent);
          forumMetricsService.incrementDbOperation("write", "MessageRepository.save");
          return repository.save(reply);
        })
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public List<Message> getReplies(UUID parentId) {
    forumMetricsService.incrementDbOperation("read", "MessageRepository.findByParentIdOrderByCreatedAtAsc");
    return repository.findByParentIdOrderByCreatedAtAsc(parentId);
  }

  @Transactional(readOnly = true)
  public Message findByIdWithReplies(UUID id) {
    forumMetricsService.incrementDbOperation("read", "MessageRepository.findByIdWithReplies");
    return repository.findByIdWithReplies(id).orElse(null);
  }

  @Transactional(readOnly = true)
  public Map<UUID, Long> getReplyCountsByParentIds(List<UUID> parentIds) {
    Map<UUID, Long> counts = new HashMap<>();
    if (parentIds == null || parentIds.isEmpty()) {
      return counts;
    }

    forumMetricsService.incrementDbOperation("read", "MessageRepository.countRepliesByParentIds");
    List<Object[]> rows = repository.countRepliesByParentIds(parentIds);
    for (Object[] row : rows) {
      UUID parentId = (UUID) row[0];
      Long count = (Long) row[1];
      counts.put(parentId, count);
    }
    return counts;
  }
}
