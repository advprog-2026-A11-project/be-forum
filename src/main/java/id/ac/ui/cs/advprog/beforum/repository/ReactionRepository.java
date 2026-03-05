package id.ac.ui.cs.advprog.beforum.repository;

import id.ac.ui.cs.advprog.beforum.model.Reaction;
import id.ac.ui.cs.advprog.beforum.model.ReactionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

  @Query("SELECT r FROM Reaction r WHERE r.message.id = :messageId ORDER BY r.createdAt ASC")
  List<Reaction> findByMessageIdOrderByCreatedAtAsc(@Param("messageId") UUID messageId);

  @Query("SELECT r FROM Reaction r "
      + "WHERE r.message.id = :messageId "
      + "AND r.userId = :userId "
      + "AND r.reactionType = :reactionType")
  Optional<Reaction> findByMessageIdAndUserIdAndReactionType(
      @Param("messageId") UUID messageId,
      @Param("userId") String userId,
      @Param("reactionType") ReactionType reactionType);

  @Query("SELECT r FROM Reaction r WHERE r.message.id = :messageId AND r.userId = :userId")
  List<Reaction> findByMessageIdAndUserId(
      @Param("messageId") UUID messageId,
      @Param("userId") String userId);

  @Query("SELECT COUNT(r) FROM Reaction r "
      + "WHERE r.message.id = :messageId "
      + "AND r.reactionType = :reactionType")
  long countByMessageIdAndReactionType(
      @Param("messageId") UUID messageId,
      @Param("reactionType") ReactionType reactionType);
}
