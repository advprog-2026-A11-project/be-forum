package id.ac.ui.cs.advprog.beforum.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ReactionTypeTest {

  @Test
  void upvoteShouldHaveCorrectDisplayValue() {
    assertEquals("upvote", ReactionType.UPVOTE.getDisplayValue());
  }

  @Test
  void downvoteShouldHaveCorrectDisplayValue() {
    assertEquals("downvote", ReactionType.DOWNVOTE.getDisplayValue());
  }

  @Test
  void fireShouldHaveCorrectDisplayValue() {
    assertEquals("🔥", ReactionType.FIRE.getDisplayValue());
  }

  @Test
  void rocketShouldHaveCorrectDisplayValue() {
    assertEquals("🚀", ReactionType.ROCKET.getDisplayValue());
  }

  @Test
  void laughShouldHaveCorrectDisplayValue() {
    assertEquals("😂", ReactionType.LAUGH.getDisplayValue());
  }

  @Test
  void partyShouldHaveCorrectDisplayValue() {
    assertEquals("🎉", ReactionType.PARTY.getDisplayValue());
  }

  @Test
  void thinkingShouldHaveCorrectDisplayValue() {
    assertEquals("🤔", ReactionType.THINKING.getDisplayValue());
  }

  @Test
  void allReactionTypesShouldExist() {
    ReactionType[] types = ReactionType.values();
    assertEquals(7, types.length);
    assertNotNull(ReactionType.valueOf("UPVOTE"));
    assertNotNull(ReactionType.valueOf("DOWNVOTE"));
    assertNotNull(ReactionType.valueOf("FIRE"));
    assertNotNull(ReactionType.valueOf("ROCKET"));
    assertNotNull(ReactionType.valueOf("LAUGH"));
    assertNotNull(ReactionType.valueOf("PARTY"));
    assertNotNull(ReactionType.valueOf("THINKING"));
  }
}
