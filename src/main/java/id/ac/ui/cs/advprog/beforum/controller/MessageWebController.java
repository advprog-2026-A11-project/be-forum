package id.ac.ui.cs.advprog.beforum.controller;

import id.ac.ui.cs.advprog.beforum.model.Message;
import id.ac.ui.cs.advprog.beforum.service.MessageService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/forum")
public class MessageWebController {

  private final MessageService service;

  public MessageWebController(MessageService service) {
    this.service = service;
  }

  @GetMapping
  public String list(Model model) {
    List<Message> messages = service.listMessages();
    model.addAttribute("messages", messages);
    return "messages";
  }

  @PostMapping("/create")
  public String create(@RequestParam String content) {
    service.createMessage(content);
    return "redirect:/forum";
  }

  @PostMapping("/update")
  public String update(@RequestParam String id, @RequestParam String content) {
    try {
      UUID uuid = UUID.fromString(id);
      service.updateMessage(uuid, content);
    } catch (IllegalArgumentException e) {
      // ignore invalid id
    }
    return "redirect:/forum";
  }

  @PostMapping("/delete")
  public String delete(@RequestParam String id) {
    try {
      UUID uuid = UUID.fromString(id);
      service.deleteMessage(uuid);
    } catch (IllegalArgumentException e) {
      // ignore
    }
    return "redirect:/forum";
  }
}
