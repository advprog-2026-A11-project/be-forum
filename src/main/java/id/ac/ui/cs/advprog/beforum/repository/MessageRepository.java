package id.ac.ui.cs.advprog.beforum.repository;

import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

  @Query("SELECT m FROM Message m WHERE m.parent.id = :parentId ORDER BY m.createdAt ASC")
  List<Message> findByParentIdOrderByCreatedAtAsc(@Param("parentId") UUID parentId);
}
