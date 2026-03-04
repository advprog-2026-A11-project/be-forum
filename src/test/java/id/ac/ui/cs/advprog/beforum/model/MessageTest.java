package id.ac.ui.cs.advprog.beforum.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void getParentId_ShouldReturnNullWhenNoParent() {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setContent("Test content");

        assertNull(message.getParentId());
    }

    @Test
    void getParentId_ShouldReturnParentIdWhenParentExists() {
        Message parent = new Message();
        UUID parentId = UUID.randomUUID();
        parent.setId(parentId);
        parent.setContent("Parent content");

        Message reply = new Message();
        reply.setId(UUID.randomUUID());
        reply.setContent("Reply content");
        reply.setParent(parent);

        assertEquals(parentId, reply.getParentId());
    }

    @Test
    void replies_ShouldBeEmptyByDefault() {
        Message message = new Message();
        
        assertNotNull(message.getReplies());
        assertTrue(message.getReplies().isEmpty());
    }

    @Test
    void addReply_ShouldAddToRepliesList() {
        Message parent = new Message();
        parent.setId(UUID.randomUUID());
        parent.setContent("Parent content");

        Message reply = new Message();
        reply.setId(UUID.randomUUID());
        reply.setContent("Reply content");
        reply.setParent(parent);
        parent.getReplies().add(reply);

        assertEquals(1, parent.getReplies().size());
        assertEquals(reply, parent.getReplies().get(0));
    }

    @Test
    void nestedReplies_ShouldWork() {
        // Create parent message
        Message parent = new Message();
        parent.setId(UUID.randomUUID());
        parent.setContent("Parent content");

        // Create first level reply
        Message reply1 = new Message();
        reply1.setId(UUID.randomUUID());
        reply1.setContent("First level reply");
        reply1.setParent(parent);
        parent.getReplies().add(reply1);

        // Create second level reply (reply to a reply)
        Message reply2 = new Message();
        reply2.setId(UUID.randomUUID());
        reply2.setContent("Second level reply");
        reply2.setParent(reply1);
        reply1.getReplies().add(reply2);

        // Verify nested structure
        assertEquals(1, parent.getReplies().size());
        assertEquals(1, reply1.getReplies().size());
        assertEquals(parent.getId(), reply1.getParentId());
        assertEquals(reply1.getId(), reply2.getParentId());
    }

    @Test
    void message_ShouldHaveCreatedAtTimestamp() {
        OffsetDateTime before = OffsetDateTime.now();
        Message message = new Message();
        OffsetDateTime after = OffsetDateTime.now();

        assertNotNull(message.getCreatedAt());
        assertTrue(message.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(message.getCreatedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void message_ShouldHaveUUID() {
        Message message = new Message();
        
        assertNotNull(message.getId());
    }
}
