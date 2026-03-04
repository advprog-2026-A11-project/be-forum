package id.ac.ui.cs.advprog.beforum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService service;

    @Autowired
    private ObjectMapper objectMapper;

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
    void createReply_ShouldReturnCreatedReply() throws Exception {
        when(service.createReply(eq(parentId), eq("Reply content"))).thenReturn(reply);

        mockMvc.perform(post("/messages/{parentId}/replies", parentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Reply content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(replyId.toString()))
                .andExpect(jsonPath("$.content").value("Reply content"))
                .andExpect(jsonPath("$.parentId").value(parentId.toString()));

        verify(service).createReply(parentId, "Reply content");
    }

    @Test
    void createReply_ShouldReturn404WhenParentNotFound() throws Exception {
        when(service.createReply(eq(parentId), any())).thenReturn(null);

        mockMvc.perform(post("/messages/{parentId}/replies", parentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Reply content\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReplies_ShouldReturnListOfReplies() throws Exception {
        Message reply2 = new Message();
        reply2.setId(UUID.randomUUID());
        reply2.setContent("Second reply");
        reply2.setParent(parentMessage);

        List<Message> replies = Arrays.asList(reply, reply2);
        when(service.findById(parentId)).thenReturn(parentMessage);
        when(service.getReplies(parentId)).thenReturn(replies);

        mockMvc.perform(get("/messages/{parentId}/replies", parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Reply content"))
                .andExpect(jsonPath("$[1].content").value("Second reply"));
    }

    @Test
    void getReplies_ShouldReturn404WhenParentNotFound() throws Exception {
        when(service.findById(parentId)).thenReturn(null);

        mockMvc.perform(get("/messages/{parentId}/replies", parentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReply_ShouldReturnUpdatedReply() throws Exception {
        String newContent = "Updated reply content";
        Message updatedReply = new Message();
        updatedReply.setId(replyId);
        updatedReply.setContent(newContent);
        updatedReply.setParent(parentMessage);

        when(service.findById(replyId)).thenReturn(reply);
        when(service.updateReply(eq(replyId), eq(newContent))).thenReturn(updatedReply);

        mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", parentId, replyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Updated reply content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(newContent));

        verify(service).updateReply(replyId, newContent);
    }

    @Test
    void updateReply_ShouldReturn404WhenReplyNotFound() throws Exception {
        when(service.findById(replyId)).thenReturn(null);

        mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", parentId, replyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Updated content\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReply_ShouldReturn404WhenReplyNotBelongsToParent() throws Exception {
        UUID wrongParentId = UUID.randomUUID();
        when(service.findById(replyId)).thenReturn(reply);

        mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", wrongParentId, replyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Updated content\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReply_ShouldReturn204() throws Exception {
        when(service.findById(replyId)).thenReturn(reply);

        mockMvc.perform(delete("/messages/{parentId}/replies/{replyId}", parentId, replyId))
                .andExpect(status().isNoContent());

        verify(service).deleteReply(replyId);
    }

    @Test
    void deleteReply_ShouldReturn404WhenReplyNotFound() throws Exception {
        when(service.findById(replyId)).thenReturn(null);

        mockMvc.perform(delete("/messages/{parentId}/replies/{replyId}", parentId, replyId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReply_ShouldReturn404WhenReplyNotBelongsToParent() throws Exception {
        UUID wrongParentId = UUID.randomUUID();
        when(service.findById(replyId)).thenReturn(reply);

        mockMvc.perform(delete("/messages/{parentId}/replies/{replyId}", wrongParentId, replyId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_ShouldReturnMessageWithReplies() throws Exception {
        parentMessage.getReplies().add(reply);
        when(service.findByIdWithReplies(parentId)).thenReturn(parentMessage);

        mockMvc.perform(get("/messages/{id}", parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parentId.toString()))
                .andExpect(jsonPath("$.content").value("Parent message content"))
                .andExpect(jsonPath("$.replies.length()").value(1))
                .andExpect(jsonPath("$.replies[0].content").value("Reply content"));
    }

    @Test
    void getById_ShouldReturn404WhenMessageNotFound() throws Exception {
        when(service.findByIdWithReplies(parentId)).thenReturn(null);

        mockMvc.perform(get("/messages/{id}", parentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createNestedReply_ShouldWork() throws Exception {
        // Test that replying to a reply works
        Message nestedReply = new Message();
        nestedReply.setId(UUID.randomUUID());
        nestedReply.setContent("Nested reply content");
        nestedReply.setParent(reply);

        when(service.createReply(eq(replyId), eq("Nested reply content"))).thenReturn(nestedReply);

        mockMvc.perform(post("/messages/{parentId}/replies", replyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"Nested reply content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Nested reply content"))
                .andExpect(jsonPath("$.parentId").value(replyId.toString()));
    }
}
