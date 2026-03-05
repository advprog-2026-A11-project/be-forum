package id.ac.ui.cs.advprog.beforum.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import id.ac.ui.cs.advprog.beforum.service.ReactionService;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReactionController.class)
class ReactionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ReactionService service;

  @Autowired
  private ObjectMapper objectMapper;

  private Message message;
  private Reaction reaction;
  private UUID messageId;
  private UUID reactionId;
  private String userId;

  @BeforeEach
  void setUp() {
    messageId = UUID.randomUUID();
    reactionId = UUID.randomUUID();
    userId = "user123";

    message = new Message();
    message.setId(messageId);
    message.setContent("Test message content");
    message.setCreatedAt(OffsetDateTime.now());

    reaction = new Reaction();
    reaction.setId(reactionId);
    reaction.setReactionType(ReactionType.UPVOTE);
    reaction.setUserId(userId);
    reaction.setMessage(message);
    reaction.setCreatedAt(OffsetDateTime.now());
  }

  @Test
  void addReactionShouldReturnCreatedReaction() throws Exception {
    when(service.addReaction(
        eq(messageId),
        eq(userId),
        eq(ReactionType.UPVOTE)
    )).thenReturn(reaction);

    mockMvc.perform(post("/messages/{messageId}/reactions", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"user123\", \"reactionType\": \"UPVOTE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reactionId.toString()))
        .andExpect(jsonPath("$.reactionType").value("UPVOTE"))
        .andExpect(jsonPath("$.userId").value(userId));

    verify(service).addReaction(messageId, userId, ReactionType.UPVOTE);
  }

  @Test
  void addReactionShouldReturn404WhenMessageNotFound() throws Exception {
    when(service.addReaction(eq(messageId), any(), any())).thenReturn(null);

    mockMvc.perform(post("/messages/{messageId}/reactions", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"user123\", \"reactionType\": \"UPVOTE\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void addReactionShouldReturn409WhenDuplicate() throws Exception {
    when(service.addReaction(eq(messageId), eq(userId), eq(ReactionType.UPVOTE)))
        .thenThrow(new IllegalStateException("User has already given this reaction"));

    mockMvc.perform(post("/messages/{messageId}/reactions", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"user123\", \"reactionType\": \"UPVOTE\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void addEmojiReactionShouldWork() throws Exception {
    reaction.setReactionType(ReactionType.FIRE);
    when(service.addReaction(
        eq(messageId),
        eq(userId),
        eq(ReactionType.FIRE)
    )).thenReturn(reaction);

    mockMvc.perform(post("/messages/{messageId}/reactions", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"user123\", \"reactionType\": \"FIRE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reactionType").value("FIRE"));

    verify(service).addReaction(messageId, userId, ReactionType.FIRE);
  }

  @Test
  void removeReactionShouldReturn204() throws Exception {
    when(service.removeReaction(
        eq(messageId),
        eq(userId),
        eq(ReactionType.UPVOTE)
    )).thenReturn(true);

    mockMvc.perform(delete("/messages/{messageId}/reactions", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"user123\", \"reactionType\": \"UPVOTE\"}"))
        .andExpect(status().isNoContent());

    verify(service).removeReaction(messageId, userId, ReactionType.UPVOTE);
  }

  @Test
  void removeReactionShouldReturn404WhenReactionNotFound() throws Exception {
    when(service.removeReaction(
        eq(messageId),
        eq(userId),
        eq(ReactionType.UPVOTE)
    )).thenReturn(false);

    mockMvc.perform(delete("/messages/{messageId}/reactions", messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"user123\", \"reactionType\": \"UPVOTE\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getReactionsShouldReturnListOfReactions() throws Exception {
    Reaction reaction2 = new Reaction();
    reaction2.setId(UUID.randomUUID());
    reaction2.setReactionType(ReactionType.FIRE);
    reaction2.setUserId("user456");
    reaction2.setMessage(message);

    List<Reaction> reactions = Arrays.asList(reaction, reaction2);
    when(service.getReactionsByMessageId(messageId)).thenReturn(reactions);

    mockMvc.perform(get("/messages/{messageId}/reactions", messageId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].reactionType").value("UPVOTE"))
        .andExpect(jsonPath("$[1].reactionType").value("FIRE"));
  }

  @Test
  void getReactionCountsShouldReturnCounts() throws Exception {
    Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
    counts.put(ReactionType.UPVOTE, 5L);
    counts.put(ReactionType.DOWNVOTE, 2L);
    counts.put(ReactionType.FIRE, 3L);
    counts.put(ReactionType.ROCKET, 0L);
    counts.put(ReactionType.LAUGH, 1L);
    counts.put(ReactionType.PARTY, 0L);
    counts.put(ReactionType.THINKING, 0L);

    when(service.getReactionCountsByMessageId(messageId)).thenReturn(counts);

    mockMvc.perform(get("/messages/{messageId}/reactions/counts", messageId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.UPVOTE").value(5))
        .andExpect(jsonPath("$.DOWNVOTE").value(2))
        .andExpect(jsonPath("$.FIRE").value(3))
        .andExpect(jsonPath("$.ROCKET").value(0))
        .andExpect(jsonPath("$.LAUGH").value(1));
  }

  @Test
  void getUserReactionsShouldReturnUserReactions() throws Exception {
    List<Reaction> reactions = Arrays.asList(reaction);
    when(service.getUserReactionsOnMessage(messageId, userId)).thenReturn(reactions);

    mockMvc.perform(get("/messages/{messageId}/reactions/user/{userId}", messageId, userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].userId").value(userId))
        .andExpect(jsonPath("$[0].reactionType").value("UPVOTE"));
  }

  @Test
  void addAllReactionTypesShouldWork() throws Exception {
    for (ReactionType reactionType : ReactionType.values()) {
      Reaction typedReaction = new Reaction();
      typedReaction.setId(UUID.randomUUID());
      typedReaction.setReactionType(reactionType);
      typedReaction.setUserId(userId);
      typedReaction.setMessage(message);

      when(service.addReaction(
          eq(messageId),
          eq(userId),
          eq(reactionType)
      )).thenReturn(typedReaction);

      mockMvc.perform(post("/messages/{messageId}/reactions", messageId)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"userId\": \"user123\", "
                  + "\"reactionType\": \"" + reactionType.name() + "\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.reactionType").value(reactionType.name()));
    }
  }

  @Test
  void getReactionsWithApiPathShouldWork() throws Exception {
    List<Reaction> reactions = Arrays.asList(reaction);
    when(service.getReactionsByMessageId(messageId)).thenReturn(reactions);

    mockMvc.perform(get("/api/messages/{messageId}/reactions", messageId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }
}
