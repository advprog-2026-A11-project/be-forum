package id.ac.ui.cs.advprog.beforum.repository;

import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
}
