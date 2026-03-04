package id.ac.ui.cs.advprog.beforum.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import id.ac.ui.cs.advprog.beforum.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
  
  List<Message> findByParent_IdOrderByCreatedAtAsc(UUID parentId);
}
