package id.ac.ui.cs.advprog.beforum.controller.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageResponseMapperTest {

  private final MessageResponseMapper mapper = new MessageResponseMapper();

  @Test
  void toResponsesShouldMapList() {
    Message message = message("m1", null);

    List<MessageResponse> responses = mapper.toResponses(List.of(message));

    assertEquals(1, responses.size());
    assertEquals(message.getId(), responses.get(0).id());
  }

  @Test
  void toResponseShouldHandleNullRepliesAndNullReactions() {
    Message message = message("m1", null);
    message.setReplies(null);
    message.setReactions(null);

    MessageResponse response = mapper.toResponse(message);

    assertEquals(0L, response.replyCount());
    assertNull(response.replies());
    assertNotNull(response.reactions());
    assertEquals(0, response.reactions().size());
    assertEquals(0L, response.reactionCounts().get(ReactionType.UPVOTE));
  }

  @Test
  void toResponseShouldMapRepliesAndReactionCounts() {
    Message parent = message("parent", null);
    Message reply = message("reply", parent);
    parent.setReplies(List.of(reply));

    Reaction r1 = reaction(parent, ReactionType.FIRE);
    Reaction r2 = reaction(parent, ReactionType.FIRE);
    Reaction r3 = reaction(parent, ReactionType.UPVOTE);
    parent.setReactions(List.of(r1, r2, r3));

    MessageResponse response = mapper.toResponse(parent);

    assertEquals(1L, response.replyCount());
    assertNotNull(response.replies());
    assertEquals(1, response.replies().size());
    assertEquals(3, response.reactions().size());
    assertEquals(2L, response.reactionCounts().get(ReactionType.FIRE));
    assertEquals(1L, response.reactionCounts().get(ReactionType.UPVOTE));
  }

  @Test
  void toResponsesShallowShouldUseReplyCountsAndNoRepliesPayload() {
    Message message = message("m1", null);
    message.setReactions(List.of(reaction(message, ReactionType.ROCKET)));

    List<MessageResponse> responses = mapper.toResponsesShallow(
        List.of(message),
        Map.of(message.getId(), 7L));

    MessageResponse response = responses.get(0);
    assertEquals(7L, response.replyCount());
    assertNull(response.replies());
    assertEquals(1, response.reactions().size());
    assertEquals(1L, response.reactionCounts().get(ReactionType.ROCKET));
  }

  private Message message(String content, Message parent) {
    Message message = new Message();
    message.setId(UUID.randomUUID());
    message.setContent(content);
    message.setReadingId("reading-1");
    message.setUserId(UUID.randomUUID());
    message.setCreatedAt(OffsetDateTime.now());
    message.setParent(parent);
    return message;
  }

  private Reaction reaction(Message message, ReactionType type) {
    Reaction reaction = new Reaction();
    reaction.setId(UUID.randomUUID());
    reaction.setReactionType(type);
    reaction.setUserId(UUID.randomUUID());
    reaction.setCreatedAt(OffsetDateTime.now());
    reaction.setMessage(message);
    return reaction;
  }
}
