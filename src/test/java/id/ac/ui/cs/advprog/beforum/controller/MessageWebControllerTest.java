package id.ac.ui.cs.advprog.beforum.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageWebController.class)
class MessageWebControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void forumShouldRedirectToRoot() throws Exception {
    mockMvc.perform(get("/forum"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }
}
