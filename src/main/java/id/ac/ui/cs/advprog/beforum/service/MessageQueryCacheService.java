package id.ac.ui.cs.advprog.beforum.service;

import id.ac.ui.cs.advprog.beforum.controller.support.MessageResponseMapper;
import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageQueryCacheService {

  private final MessageService messageService;
  private final MessageResponseMapper responseMapper;

  public MessageQueryCacheService(
      MessageService messageService,
      MessageResponseMapper responseMapper) {
    this.messageService = messageService;
    this.responseMapper = responseMapper;
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "messages:list:dto", key = "#readingId == null || #readingId.isBlank() ? 'ALL' : #readingId.trim()")
  public List<MessageResponse> listMessagesDto(String readingId) {
    List<Message> messages = messageService.listMessages(readingId);
    List<UUID> messageIds = messages.stream().map(Message::getId).toList();
    Map<UUID, Long> replyCounts = messageService.getReplyCountsByParentIds(messageIds);
    return responseMapper.toResponsesShallow(messages, replyCounts);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "messages:detail:dto", key = "#id", unless = "#result == null")
  public MessageResponse getMessageDetailDto(UUID id) {
    Message message = messageService.findByIdWithReplies(id);
    if (message == null) {
      return null;
    }
    return responseMapper.toResponse(message);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "messages:replies:dto", key = "#parentId", unless = "#result == null")
  public List<MessageResponse> getRepliesDto(UUID parentId) {
    Message parent = messageService.findById(parentId);
    if (parent == null) {
      return null;
    }
    return responseMapper.toResponses(messageService.getReplies(parentId));
  }
}
