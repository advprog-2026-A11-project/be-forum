package id.ac.ui.cs.advprog.beforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock
  private MessageRepository repository;

  @InjectMocks
  private MessageService service;

  private Message parentMessage;
  private Message reply;
  private UUID parentId;
  private UUID replyId;
  private UUID userId;

  @BeforeEach
  void setUp() {
    parentId = UUID.randomUUID();
    replyId = UUID.randomUUID();
    userId = UUID.randomUUID();

    parentMessage = new Message();
    parentMessage.setId(parentId);
    parentMessage.setContent("Parent message content");
    parentMessage.setReadingId("reading-1");
    parentMessage.setUserId(userId);
    parentMessage.setCreatedAt(OffsetDateTime.now());

    reply = new Message();
    reply.setId(replyId);
    reply.setContent("Reply content");
    reply.setReadingId("reading-1");
    reply.setUserId(userId);
    reply.setCreatedAt(OffsetDateTime.now());
    reply.setParent(parentMessage);
  }

  @Test
  void createMessageShouldCreateMessage() {
    String content = "New message";
    String readingId = "reading-123";
    when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Message created = service.createMessage(content, readingId, userId);

    assertNotNull(created);
    assertEquals(content, created.getContent());
    assertEquals(readingId, created.getReadingId());
    assertEquals(userId, created.getUserId());
    assertNull(created.getParent());
    verify(repository).save(any(Message.class));
  }

  @Test
  void createMessageShouldRejectMissingReadingId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.createMessage("New message", " ", userId));
    verify(repository, never()).save(any(Message.class));
  }

  @Test
  void createMessageShouldRejectNullReadingId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.createMessage("New message", null, userId));
    verify(repository, never()).save(any(Message.class));
  }

  @Test
  void createReplyShouldCreateReplyWithParent() {
    String replyContent = "This is a reply";
    when(repository.findById(parentId)).thenReturn(Optional.of(parentMessage));
    when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Message createdReply = service.createReply(parentId, replyContent, userId);

    assertNotNull(createdReply);
    assertEquals(replyContent, createdReply.getContent());
    assertEquals(parentMessage, createdReply.getParent());
    assertEquals(parentId, createdReply.getParentId());
    assertEquals(parentMessage.getReadingId(), createdReply.getReadingId());
    assertEquals(userId, createdReply.getUserId());
    verify(repository).findById(parentId);
    verify(repository).save(any(Message.class));
  }

  @Test
  void createReplyShouldReturnNullWhenParentNotFound() {
    UUID nonExistentParentId = UUID.randomUUID();
    when(repository.findById(nonExistentParentId)).thenReturn(Optional.empty());

    Message createdReply = service.createReply(nonExistentParentId, "Reply content", userId);

    assertNull(createdReply);
    verify(repository).findById(nonExistentParentId);
    verify(repository, never()).save(any(Message.class));
  }

  @Test
  void createReplyShouldAllowNestedReplies() {
    Message nestedReply = new Message();
    nestedReply.setId(UUID.randomUUID());
    nestedReply.setContent("Nested reply content");
    nestedReply.setParent(reply);
    nestedReply.setCreatedAt(OffsetDateTime.now());

    when(repository.findById(replyId)).thenReturn(Optional.of(reply));
    when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Message createdNestedReply = service.createReply(replyId, "Nested reply content", userId);

    assertNotNull(createdNestedReply);
    assertEquals(reply, createdNestedReply.getParent());
    assertEquals(replyId, createdNestedReply.getParentId());
    assertEquals(reply.getReadingId(), createdNestedReply.getReadingId());
    assertEquals(userId, createdNestedReply.getUserId());
  }

  @Test
  void getRepliesShouldReturnRepliesForParent() {
    Message reply2 = new Message();
    reply2.setId(UUID.randomUUID());
    reply2.setContent("Second reply");
    reply2.setReadingId("reading-1");
    reply2.setParent(parentMessage);

    List<Message> replies = Arrays.asList(reply, reply2);
    when(repository.findByParentIdOrderByCreatedAtAsc(parentId)).thenReturn(replies);

    List<Message> result = service.getReplies(parentId);

    assertEquals(2, result.size());
    assertEquals(reply, result.get(0));
    assertEquals(reply2, result.get(1));
    verify(repository).findByParentIdOrderByCreatedAtAsc(parentId);
  }

  @Test
  void getRepliesShouldReturnEmptyListWhenNoReplies() {
    when(repository.findByParentIdOrderByCreatedAtAsc(parentId)).thenReturn(List.of());

    List<Message> result = service.getReplies(parentId);

    assertTrue(result.isEmpty());
  }

  @Test
  void updateMessageShouldUpdateMessageContent() {
    String newContent = "Updated message content";
    when(repository.findById(parentId)).thenReturn(Optional.of(parentMessage));
    when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Message updated = service.updateMessage(parentId, newContent);

    assertNotNull(updated);
    assertEquals(newContent, updated.getContent());
    verify(repository).findById(parentId);
    verify(repository).save(any(Message.class));
  }

  @Test
  void updateMessageShouldReturnNullWhenNotFound() {
    UUID nonExistentId = UUID.randomUUID();
    when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

    Message updated = service.updateMessage(nonExistentId, "New content");

    assertNull(updated);
  }

  @Test
  void deleteMessageShouldDeleteMessage() {
    service.deleteMessage(parentId);

    verify(repository).deleteById(parentId);
  }

  @Test
  void findByIdShouldReturnMessage() {
    when(repository.findById(parentId)).thenReturn(Optional.of(parentMessage));

    Message found = service.findById(parentId);

    assertNotNull(found);
    assertEquals(parentId, found.getId());
  }

  @Test
  void findByIdShouldReturnNullWhenNotFound() {
    UUID nonExistentId = UUID.randomUUID();
    when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

    Message found = service.findById(nonExistentId);

    assertNull(found);
  }

  @Test
  void listMessagesShouldReturnAllMessages() {
    Message msg1 = new Message();
    msg1.setId(UUID.randomUUID());
    msg1.setContent("Message 1");
    msg1.setReadingId("reading-1");

    Message msg2 = new Message();
    msg2.setId(UUID.randomUUID());
    msg2.setContent("Message 2");
    msg2.setReadingId("reading-2");

    List<Message> messages = Arrays.asList(msg1, msg2);
    when(repository.findTopLevelOrderByCreatedAtDesc()).thenReturn(messages);

    List<Message> result = service.listMessages(null);

    assertEquals(2, result.size());
    verify(repository).findTopLevelOrderByCreatedAtDesc();
  }

  @Test
  void listMessagesShouldFilterByReadingId() {
    Message msg = new Message();
    msg.setId(UUID.randomUUID());
    msg.setContent("Message 1");
    msg.setReadingId("reading-1");

    when(repository.findTopLevelByReadingIdOrderByCreatedAtDesc("reading-1"))
        .thenReturn(List.of(msg));

    List<Message> result = service.listMessages("reading-1");

    assertEquals(1, result.size());
    assertEquals("reading-1", result.get(0).getReadingId());
    verify(repository).findTopLevelByReadingIdOrderByCreatedAtDesc("reading-1");
  }

  @Test
  void listMessagesShouldTreatBlankReadingIdAsUnfiltered() {
    Message msg = new Message();
    msg.setId(UUID.randomUUID());
    msg.setContent("Message 1");
    msg.setReadingId("reading-1");

    when(repository.findTopLevelOrderByCreatedAtDesc()).thenReturn(List.of(msg));

    List<Message> result = service.listMessages("   ");

    assertEquals(1, result.size());
    verify(repository).findTopLevelOrderByCreatedAtDesc();
  }

  @Test
  void findByIdWithRepliesShouldReturnMessageWithReplies() {
    parentMessage.getReplies().add(reply);
    when(repository.findByIdWithReplies(parentId)).thenReturn(Optional.of(parentMessage));

    Message result = service.findByIdWithReplies(parentId);

    assertNotNull(result);
    assertEquals(parentId, result.getId());
    assertEquals(1, result.getReplies().size());
  }

  @Test
  void findByIdWithRepliesShouldReturnNullWhenNotFound() {
    UUID nonExistentId = UUID.randomUUID();
    when(repository.findByIdWithReplies(nonExistentId)).thenReturn(Optional.empty());

    Message result = service.findByIdWithReplies(nonExistentId);

    assertNull(result);
  }

  @Test
  void loadRepliesRecursivelyShouldLoadNestedReplies() {
    Message nestedReply = new Message();
    nestedReply.setId(UUID.randomUUID());
    nestedReply.setContent("Nested reply");
    nestedReply.setParent(reply);

    reply.getReplies().add(nestedReply);
    parentMessage.getReplies().add(reply);

    when(repository.findByIdWithReplies(parentId)).thenReturn(Optional.of(parentMessage));

    Message result = service.findByIdWithReplies(parentId);

    assertNotNull(result);
    assertEquals(1, result.getReplies().size());
    assertEquals(1, result.getReplies().get(0).getReplies().size());
  }

  @Test
  void loadRepliesRecursivelyShouldHandleNullReplies() {
    Message messageWithNullReplies = new Message();
    messageWithNullReplies.setId(UUID.randomUUID());
    messageWithNullReplies.setContent("Message without replies");
    messageWithNullReplies.setReplies(null);

    when(repository.findByIdWithReplies(messageWithNullReplies.getId())).thenReturn(
        Optional.of(messageWithNullReplies));

    Message result = service.findByIdWithReplies(messageWithNullReplies.getId());

    assertNotNull(result);
    assertNull(result.getReplies());
  }
}
