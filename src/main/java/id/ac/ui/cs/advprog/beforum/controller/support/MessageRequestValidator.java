package id.ac.ui.cs.advprog.beforum.controller.support;

import id.ac.ui.cs.advprog.beforum.dto.CreateMessageRequest;
import org.springframework.stereotype.Component;

@Component
public class MessageRequestValidator {

  public boolean hasValidReadingId(CreateMessageRequest request) {
    return request != null && request.readingId() != null && !request.readingId().isBlank();
  }
}
