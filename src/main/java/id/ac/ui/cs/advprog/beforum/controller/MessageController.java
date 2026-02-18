package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.service.MessageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
public class MessageController {

  private final MessageService service;

  public MessageController(MessageService service) {
    this.service = service;
  }

  public static record CreateMessageRequest(String content) {}

  @PostMapping
  public ResponseEntity<Message> create(@RequestBody CreateMessageRequest req) {
    Message created = service.createMessage(req.content());
    return ResponseEntity.ok(created);
  }

  @GetMapping
  public ResponseEntity<List<Message>> list() {
    return ResponseEntity.ok(service.listMessages());
  }
}
