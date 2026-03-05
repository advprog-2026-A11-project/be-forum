package id.ac.ui.cs.advprog.beforum.model;

public enum ReactionType {
  UPVOTE("upvote"),
  DOWNVOTE("downvote"),
  FIRE("🔥"),
  ROCKET("🚀"),
  LAUGH("😂"),
  PARTY("🎉"),
  THINKING("🤔");

  private final String displayValue;

  ReactionType(String displayValue) {
    this.displayValue = displayValue;
  }

  public String getDisplayValue() {
    return displayValue;
  }
}
