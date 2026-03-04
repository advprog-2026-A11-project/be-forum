package id.ac.ui.cs.advprog.beforum.service;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        parentId = UUID.randomUUID();
        replyId = UUID.randomUUID();

        parentMessage = new Message();
        parentMessage.setId(parentId);
        parentMessage.setContent("Parent message content");
        parentMessage.setCreatedAt(OffsetDateTime.now());

        reply = new Message();
        reply.setId(replyId);
        reply.setContent("Reply content");
        reply.setCreatedAt(OffsetDateTime.now());
        reply.setParent(parentMessage);
    }

    @Test
    void createMessage_ShouldCreateMessage() {
        String content = "New message";
        when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message created = service.createMessage(content);

        assertNotNull(created);
        assertEquals(content, created.getContent());
        assertNull(created.getParent());
        verify(repository).save(any(Message.class));
    }

    @Test
    void createReply_ShouldCreateReplyWithParent() {
        String replyContent = "This is a reply";
        when(repository.findById(parentId)).thenReturn(Optional.of(parentMessage));
        when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message createdReply = service.createReply(parentId, replyContent);

        assertNotNull(createdReply);
        assertEquals(replyContent, createdReply.getContent());
        assertEquals(parentMessage, createdReply.getParent());
        assertEquals(parentId, createdReply.getParentId());
        verify(repository).findById(parentId);
        verify(repository).save(any(Message.class));
    }

    @Test
    void createReply_ShouldReturnNullWhenParentNotFound() {
        UUID nonExistentParentId = UUID.randomUUID();
        when(repository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        Message createdReply = service.createReply(nonExistentParentId, "Reply content");

        assertNull(createdReply);
        verify(repository).findById(nonExistentParentId);
        verify(repository, never()).save(any(Message.class));
    }

    @Test
    void createReply_ShouldAllowNestedReplies() {
        // Create a reply to a reply (nested reply)
        Message nestedReply = new Message();
        nestedReply.setId(UUID.randomUUID());
        nestedReply.setContent("Nested reply content");
        nestedReply.setParent(reply);
        nestedReply.setCreatedAt(OffsetDateTime.now());

        when(repository.findById(replyId)).thenReturn(Optional.of(reply));
        when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message createdNestedReply = service.createReply(replyId, "Nested reply content");

        assertNotNull(createdNestedReply);
        assertEquals(reply, createdNestedReply.getParent());
        assertEquals(replyId, createdNestedReply.getParentId());
    }

    @Test
    void getReplies_ShouldReturnRepliesForParent() {
        Message reply2 = new Message();
        reply2.setId(UUID.randomUUID());
        reply2.setContent("Second reply");
        reply2.setParent(parentMessage);

        List<Message> replies = Arrays.asList(reply, reply2);
        when(repository.findByParent_IdOrderByCreatedAtAsc(parentId)).thenReturn(replies);

        List<Message> result = service.getReplies(parentId);

        assertEquals(2, result.size());
        assertEquals(reply, result.get(0));
        assertEquals(reply2, result.get(1));
        verify(repository).findByParent_IdOrderByCreatedAtAsc(parentId);
    }

    @Test
    void getReplies_ShouldReturnEmptyListWhenNoReplies() {
        when(repository.findByParent_IdOrderByCreatedAtAsc(parentId)).thenReturn(List.of());

        List<Message> result = service.getReplies(parentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void listTopLevelMessages_ShouldReturnOnlyTopLevelMessages() {
        Message topLevel1 = new Message();
        topLevel1.setId(UUID.randomUUID());
        topLevel1.setContent("Top level 1");

        Message topLevel2 = new Message();
        topLevel2.setId(UUID.randomUUID());
        topLevel2.setContent("Top level 2");

        List<Message> topLevelMessages = Arrays.asList(topLevel1, topLevel2);
        when(repository.findByParentIsNull()).thenReturn(topLevelMessages);

        List<Message> result = service.listTopLevelMessages();

        assertEquals(2, result.size());
        verify(repository).findByParentIsNull();
    }

    @Test
    void updateReply_ShouldUpdateReplyContent() {
        String newContent = "Updated reply content";
        when(repository.findById(replyId)).thenReturn(Optional.of(reply));
        when(repository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message updated = service.updateReply(replyId, newContent);

        assertNotNull(updated);
        assertEquals(newContent, updated.getContent());
        verify(repository).findById(replyId);
        verify(repository).save(any(Message.class));
    }

    @Test
    void updateReply_ShouldReturnNullWhenReplyNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        Message updated = service.updateReply(nonExistentId, "New content");

        assertNull(updated);
    }

    @Test
    void deleteReply_ShouldDeleteReply() {
        service.deleteReply(replyId);

        verify(repository).deleteById(replyId);
    }

    @Test
    void findById_ShouldReturnMessage() {
        when(repository.findById(parentId)).thenReturn(Optional.of(parentMessage));

        Message found = service.findById(parentId);

        assertNotNull(found);
        assertEquals(parentId, found.getId());
    }

    @Test
    void findById_ShouldReturnNullWhenNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        Message found = service.findById(nonExistentId);

        assertNull(found);
    }
}
