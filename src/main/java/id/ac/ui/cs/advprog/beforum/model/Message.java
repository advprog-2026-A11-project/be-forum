package id.ac.ui.cs.advprog.beforum.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id = UUID.randomUUID();

  @Version
  @Column(name = "version")
  private Integer version;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "reading_id")
  private String readingId;

  @Column(name = "user_id")
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Message parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Message> replies = new ArrayList<>();

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Reaction> reactions = new ArrayList<>();

  public Message() {
    // Required by JPA.
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getReadingId() {
    return readingId;
  }

  public void setReadingId(String readingId) {
    this.readingId = readingId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public Message getParent() {
    return parent;
  }

  public void setParent(Message parent) {
    this.parent = parent;
  }

  public List<Message> getReplies() {
    return replies;
  }

  public void setReplies(List<Message> replies) {
    this.replies = replies;
  }

  public List<Reaction> getReactions() {
    return reactions;
  }

  public void setReactions(List<Reaction> reactions) {
    this.reactions = reactions;
  }

  public UUID getParentId() {
    return parent != null ? parent.getId() : null;
  }
}
