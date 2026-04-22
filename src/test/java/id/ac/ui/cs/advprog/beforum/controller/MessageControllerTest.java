package id.ac.ui.cs.advprog.beforum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageAuthorizationService;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageRequestValidator;
import id.ac.ui.cs.advprog.beforum.controller.support.MessageResponseMapper;
import id.ac.ui.cs.advprog.beforum.controller.support.UseCaseRequestHandler;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.security.SecurityConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest({MessageController.class, MessageReplyController.class})
@Import({
    SecurityConfig.class,
    UseCaseRequestHandler.class,
    MessageRequestValidator.class,
    MessageAuthorizationService.class,
    MessageResponseMapper.class
})
class MessageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private MessageService service;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Autowired
  private ObjectMapper objectMapper;

  private Message parentMessage;
  private Message reply;
  private UUID parentId;
  private UUID replyId;
  private UUID userId;

  private RequestPostProcessor authenticatedJwt() {
    return jwt().jwt(token -> token.subject(userId.toString()));
  }

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
  void createReplyShouldReturnCreatedReply() throws Exception {
    when(service.createReply(eq(parentId), eq("Reply content"), eq(userId))).thenReturn(reply);

    mockMvc.perform(post("/api/messages/{parentId}/replies", parentId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Reply content\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(replyId.toString()))
        .andExpect(jsonPath("$.content").value("Reply content"))
        .andExpect(jsonPath("$.parentId").value(parentId.toString()));

    verify(service).createReply(eq(parentId), eq("Reply content"), eq(userId));
  }

  @Test
  void createReplyShouldReturn404WhenParentNotFound() throws Exception {
    when(service.createReply(eq(parentId), any(), eq(userId))).thenReturn(null);

    mockMvc.perform(post("/api/messages/{parentId}/replies", parentId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Reply content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void createReplyShouldReturn401WhenJwtMissing() throws Exception {
    mockMvc.perform(post("/api/messages/{parentId}/replies", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Reply content\"}"))
        .andExpect(status().isUnauthorized());
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

    mockMvc.perform(get("/api/messages/{parentId}/replies", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].content").value("Reply content"))
        .andExpect(jsonPath("$[1].content").value("Second reply"));
  }

  @Test
  void getRepliesShouldReturn404WhenParentNotFound() throws Exception {
    when(service.findById(parentId)).thenReturn(null);

    mockMvc.perform(get("/api/messages/{parentId}/replies", parentId))
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

    mockMvc.perform(put("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated reply content\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value(newContent));

    verify(service).updateMessage(replyId, newContent);
  }

  @Test
  void updateReplyShouldReturn404WhenReplyNotFound() throws Exception {
    when(service.findById(replyId)).thenReturn(null);

    mockMvc.perform(put("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReplyShouldReturn404WhenReplyNotBelongsToParent() throws Exception {
    UUID wrongParentId = UUID.randomUUID();
    when(service.findById(replyId)).thenReturn(reply);

    mockMvc.perform(put("/api/messages/{parentId}/replies/{replyId}", wrongParentId, replyId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReplyShouldReturn204() throws Exception {
    when(service.findById(replyId)).thenReturn(reply);

    mockMvc.perform(delete("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt()))
        .andExpect(status().isNoContent());

    verify(service).deleteMessage(replyId);
  }

  @Test
  void deleteReplyShouldReturn404WhenReplyNotFound() throws Exception {
    when(service.findById(replyId)).thenReturn(null);

    mockMvc.perform(delete("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReplyShouldReturn404WhenReplyNotBelongsToParent() throws Exception {
    UUID wrongParentId = UUID.randomUUID();
    when(service.findById(replyId)).thenReturn(reply);

    mockMvc.perform(delete("/api/messages/{parentId}/replies/{replyId}", wrongParentId, replyId)
            .with(authenticatedJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReplyShouldReturn403WhenUserIsNotOwner() throws Exception {
    Message ownedByAnotherUser = new Message();
    ownedByAnotherUser.setId(replyId);
    ownedByAnotherUser.setUserId(UUID.randomUUID());
    ownedByAnotherUser.setParent(parentMessage);
    when(service.findById(replyId)).thenReturn(ownedByAnotherUser);

    mockMvc.perform(delete("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt()))
        .andExpect(status().isForbidden());

    verify(service, never()).deleteMessage(any(UUID.class));
  }

  @Test
  void getByIdShouldReturnMessageWithReplies() throws Exception {
    parentMessage.getReplies().add(reply);
    when(service.findByIdWithReplies(parentId)).thenReturn(parentMessage);

    mockMvc.perform(get("/api/messages/{id}", parentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(parentId.toString()))
        .andExpect(jsonPath("$.content").value("Parent message content"))
        .andExpect(jsonPath("$.replies.length()").value(1))
        .andExpect(jsonPath("$.replies[0].content").value("Reply content"));
  }

  @Test
  void getByIdShouldReturn404WhenMessageNotFound() throws Exception {
    when(service.findByIdWithReplies(parentId)).thenReturn(null);

    mockMvc.perform(get("/api/messages/{id}", parentId))
        .andExpect(status().isNotFound());
  }

  @Test
  void createNestedReplyShouldWork() throws Exception {
    Message nestedReply = new Message();
    nestedReply.setId(UUID.randomUUID());
    nestedReply.setContent("Nested reply content");
    nestedReply.setReadingId("reading-1");
    nestedReply.setParent(reply);

    when(service.createReply(eq(replyId), eq("Nested reply content"), eq(userId)))
        .thenReturn(nestedReply);

    mockMvc.perform(post("/api/messages/{parentId}/replies", replyId)
            .with(authenticatedJwt())
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
    newMessage.setReadingId("reading-1");

    when(service.createMessage(eq("New message"), eq("reading-1"), eq(userId)))
        .thenReturn(newMessage);

    mockMvc.perform(post("/api/messages")
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\", \"readingId\": \"reading-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("New message"))
        .andExpect(jsonPath("$.readingId").value("reading-1"));

    verify(service).createMessage(eq("New message"), eq("reading-1"), eq(userId));
  }

  @Test
  void createShouldReturnBadRequestWhenReadingIdMissing() throws Exception {
    mockMvc.perform(post("/api/messages")
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createShouldReturnBadRequestWhenReadingIdBlank() throws Exception {
    mockMvc.perform(post("/api/messages")
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\", \"readingId\": \"   \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createShouldReturnBadRequestWhenServiceThrowsIllegalArgumentException() throws Exception {
    when(service.createMessage(eq("New message"), eq("reading-1"), eq(userId)))
        .thenThrow(new IllegalArgumentException("readingId is required for thread creation"));

    mockMvc.perform(post("/api/messages")
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\", \"readingId\": \"reading-1\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createShouldReturn401WhenJwtSubjectIsNotUuid() throws Exception {
    mockMvc.perform(post("/api/messages")
            .with(jwt().jwt(token -> token.subject("invalid-sub")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\", \"readingId\": \"reading-1\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createShouldReturn401WhenJwtSubjectIsBlank() throws Exception {
    mockMvc.perform(post("/api/messages")
            .with(jwt().jwt(token -> token.subject("   ")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\", \"readingId\": \"reading-1\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createShouldReturn401WhenJwtSubjectMissing() throws Exception {
    mockMvc.perform(post("/api/messages")
            .with(jwt().jwt(token -> token.claims(claims -> claims.remove("sub"))))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\", \"readingId\": \"reading-1\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createShouldReturn401WhenJwtMissing() throws Exception {
    mockMvc.perform(post("/api/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"New message\", \"readingId\": \"reading-1\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listShouldReturnAllMessages() throws Exception {
    List<Message> messages = Arrays.asList(parentMessage, reply);
    when(service.listMessages(null)).thenReturn(messages);

    mockMvc.perform(get("/api/messages"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    verify(service).listMessages(null);
  }

  @Test
  void listShouldFilterByReadingId() throws Exception {
    when(service.listMessages("reading-1")).thenReturn(List.of(parentMessage));

    mockMvc.perform(get("/api/messages").param("readingId", "reading-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].readingId").value("reading-1"));

    verify(service).listMessages("reading-1");
  }

  @Test
  void updateShouldReturnUpdatedMessage() throws Exception {
    String newContent = "Updated content";
    Message updated = new Message();
    updated.setId(parentId);
    updated.setContent(newContent);

    when(service.findById(parentId)).thenReturn(parentMessage);
    when(service.updateMessage(eq(parentId), eq(newContent))).thenReturn(updated);

    mockMvc.perform(put("/api/messages/{id}", parentId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value(newContent));

    verify(service).updateMessage(parentId, newContent);
  }

  @Test
  void updateShouldReturn404WhenNotFound() throws Exception {
    when(service.findById(parentId)).thenReturn(null);

    mockMvc.perform(put("/api/messages/{id}", parentId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateShouldReturn403WhenUserIsNotOwner() throws Exception {
    Message ownedByAnotherUser = new Message();
    ownedByAnotherUser.setId(parentId);
    ownedByAnotherUser.setUserId(UUID.randomUUID());
    when(service.findById(parentId)).thenReturn(ownedByAnotherUser);

    mockMvc.perform(put("/api/messages/{id}", parentId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isForbidden());

    verify(service, never()).updateMessage(any(UUID.class), any(String.class));
  }

  @Test
  void updateShouldReturn401WhenJwtMissing() throws Exception {
    mockMvc.perform(put("/api/messages/{id}", parentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateShouldReturn403WhenOwnerIsNull() throws Exception {
    Message ownerless = new Message();
    ownerless.setId(parentId);
    when(service.findById(parentId)).thenReturn(ownerless);

    mockMvc.perform(put("/api/messages/{id}", parentId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isForbidden());

    verify(service, never()).updateMessage(any(UUID.class), any(String.class));
  }

  @Test
  void updateShouldReturn404WhenUpdateReturnsNull() throws Exception {
    when(service.findById(parentId)).thenReturn(parentMessage);
    when(service.updateMessage(eq(parentId), eq("Updated content"))).thenReturn(null);

    mockMvc.perform(put("/api/messages/{id}", parentId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteShouldReturn204() throws Exception {
    when(service.findById(parentId)).thenReturn(parentMessage);

    mockMvc.perform(delete("/api/messages/{id}", parentId)
            .with(authenticatedJwt()))
        .andExpect(status().isNoContent());

    verify(service).deleteMessage(parentId);
  }

  @Test
  void deleteShouldReturn404WhenNotFound() throws Exception {
    when(service.findById(parentId)).thenReturn(null);

    mockMvc.perform(delete("/api/messages/{id}", parentId)
            .with(authenticatedJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteShouldReturn403WhenUserIsNotOwner() throws Exception {
    Message ownedByAnotherUser = new Message();
    ownedByAnotherUser.setId(parentId);
    ownedByAnotherUser.setUserId(UUID.randomUUID());
    when(service.findById(parentId)).thenReturn(ownedByAnotherUser);

    mockMvc.perform(delete("/api/messages/{id}", parentId)
            .with(authenticatedJwt()))
        .andExpect(status().isForbidden());

    verify(service, never()).deleteMessage(any(UUID.class));
  }

  @Test
  void deleteShouldReturn403WhenOwnerIsNull() throws Exception {
    Message ownerless = new Message();
    ownerless.setId(parentId);
    when(service.findById(parentId)).thenReturn(ownerless);

    mockMvc.perform(delete("/api/messages/{id}", parentId)
            .with(authenticatedJwt()))
        .andExpect(status().isForbidden());

    verify(service, never()).deleteMessage(any(UUID.class));
  }

  @Test
  void deleteShouldReturn401WhenJwtMissing() throws Exception {
    mockMvc.perform(delete("/api/messages/{id}", parentId))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateReplyShouldReturn404WhenReplyHasNullParentId() throws Exception {
    Message orphanReply = new Message();
    orphanReply.setId(replyId);
    orphanReply.setContent("Orphan reply");

    when(service.findById(replyId)).thenReturn(orphanReply);

    mockMvc.perform(put("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt())
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

    mockMvc.perform(delete("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReplyShouldReturn404WhenUpdateReturnsNull() throws Exception {
    when(service.findById(replyId)).thenReturn(reply);
    when(service.updateMessage(eq(replyId), any())).thenReturn(null);

    mockMvc.perform(put("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateReplyShouldReturn403WhenOwnerIsNull() throws Exception {
    Message ownerlessReply = new Message();
    ownerlessReply.setId(replyId);
    ownerlessReply.setParent(parentMessage);
    when(service.findById(replyId)).thenReturn(ownerlessReply);

    mockMvc.perform(put("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .with(authenticatedJwt())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isForbidden());

    verify(service, never()).updateMessage(any(UUID.class), any(String.class));
  }

  @Test
  void updateReplyShouldReturn401WhenJwtMissing() throws Exception {
    mockMvc.perform(put("/api/messages/{parentId}/replies/{replyId}", parentId, replyId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"content\": \"Updated content\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteReplyShouldReturn401WhenJwtMissing() throws Exception {
    mockMvc.perform(delete("/api/messages/{parentId}/replies/{replyId}", parentId, replyId))
        .andExpect(status().isUnauthorized());
  }
}
