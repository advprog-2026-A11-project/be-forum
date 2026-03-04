package id.ac.ui.cs.advprog.beforum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.service.MessageService;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
  void createReplyShouldReturnCreatedReply() throws Exception {
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
  void createReplyShouldReturn404WhenParentNotFound() throws Exception {
    when(service.createReply(eq(parentId), any())).thenReturn(null);

    mockMvc.perform(post("/messages/{parentId}/replies", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Reply content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getRepliesShouldReturnListOfReplies() throws Exception {
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
  void getRepliesShouldReturn404WhenParentNotFound() throws Exception {
    when(service.findById(parentId)).thenReturn(null);

    mockMvc.perform(get("/messages/{parentId}/replies", parentId))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReplyShouldReturnUpdatedReply() throws Exception {
    String newContent = "Updated reply content";
    Message updatedReply = new Message();
    updatedReply.setId(replyId);
    updatedReply.setContent(newContent);
    updatedReply.setParent(parentMessage);

    when(service.findById(replyId)).thenReturn(reply);
    when(service.updateMessage(eq(replyId), eq(newContent))).thenReturn(updatedReply);

    mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated reply content\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value(newContent));

    verify(service).updateMessage(replyId, newContent);
  }

  @Test
  void updateReplyShouldReturn404WhenReplyNotFound() throws Exception {
    when(service.findById(replyId)).thenReturn(null);

    mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReplyShouldReturn404WhenReplyNotBelongsToParent() throws Exception {
    UUID wrongParentId = UUID.randomUUID();
    when(service.findById(replyId)).thenReturn(reply);

    mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", wrongParentId, replyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReplyShouldReturn204() throws Exception {
    when(service.findById(replyId)).thenReturn(reply);

    mockMvc.perform(delete("/messages/{parentId}/replies/{replyId}", parentId, replyId))
        .andExpect(status().isNoContent());

    verify(service).deleteMessage(replyId);
  }

  @Test
  void deleteReplyShouldReturn404WhenReplyNotFound() throws Exception {
    when(service.findById(replyId)).thenReturn(null);

    mockMvc.perform(delete("/messages/{parentId}/replies/{replyId}", parentId, replyId))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReplyShouldReturn404WhenReplyNotBelongsToParent() throws Exception {
    UUID wrongParentId = UUID.randomUUID();
    when(service.findById(replyId)).thenReturn(reply);

    mockMvc.perform(delete("/messages/{parentId}/replies/{replyId}", wrongParentId, replyId))
        .andExpect(status().isNotFound());
  }

  @Test
  void getByIdShouldReturnMessageWithReplies() throws Exception {
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
  void getByIdShouldReturn404WhenMessageNotFound() throws Exception {
    when(service.findByIdWithReplies(parentId)).thenReturn(null);

    mockMvc.perform(get("/messages/{id}", parentId))
        .andExpect(status().isNotFound());
  }

  @Test
  void createNestedReplyShouldWork() throws Exception {
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

  @Test
  void createShouldReturnCreatedMessage() throws Exception {
    Message newMessage = new Message();
    newMessage.setId(UUID.randomUUID());
    newMessage.setContent("New message");

    when(service.createMessage("New message")).thenReturn(newMessage);

    mockMvc.perform(post("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("New message"));

    verify(service).createMessage("New message");
  }

  @Test
  void listShouldReturnAllMessages() throws Exception {
    List<Message> messages = Arrays.asList(parentMessage, reply);
    when(service.listMessages()).thenReturn(messages);

    mockMvc.perform(get("/messages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    verify(service).listMessages();
  }

  @Test
  void updateShouldReturnUpdatedMessage() throws Exception {
    String newContent = "Updated content";
    Message updated = new Message();
    updated.setId(parentId);
    updated.setContent(newContent);

    when(service.updateMessage(eq(parentId), eq(newContent))).thenReturn(updated);

    mockMvc.perform(put("/messages/{id}", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value(newContent));

    verify(service).updateMessage(parentId, newContent);
  }

  @Test
  void updateShouldReturn404WhenNotFound() throws Exception {
    when(service.updateMessage(eq(parentId), any())).thenReturn(null);

    mockMvc.perform(put("/messages/{id}", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteShouldReturn204() throws Exception {
    when(service.findById(parentId)).thenReturn(parentMessage);

    mockMvc.perform(delete("/messages/{id}", parentId))
        .andExpect(status().isNoContent());

    verify(service).deleteMessage(parentId);
  }

  @Test
  void deleteShouldReturn404WhenNotFound() throws Exception {
    when(service.findById(parentId)).thenReturn(null);

    mockMvc.perform(delete("/messages/{id}", parentId))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReplyShouldReturn404WhenReplyHasNullParentId() throws Exception {
    Message orphanReply = new Message();
    orphanReply.setId(replyId);
    orphanReply.setContent("Orphan reply");

    when(service.findById(replyId)).thenReturn(orphanReply);

    mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReplyShouldReturn404WhenReplyHasNullParentId() throws Exception {
    Message orphanReply = new Message();
    orphanReply.setId(replyId);
    orphanReply.setContent("Orphan reply");

    when(service.findById(replyId)).thenReturn(orphanReply);

    mockMvc.perform(delete("/messages/{parentId}/replies/{replyId}", parentId, replyId))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReplyShouldReturn404WhenUpdateReturnsNull() throws Exception {
    when(service.findById(replyId)).thenReturn(reply);
    when(service.updateMessage(eq(replyId), any())).thenReturn(null);

    mockMvc.perform(put("/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }
}
