package id.ac.ui.cs.advprog.beforum;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class BeForumApplicationTests {

  @MockBean
  private JwtDecoder jwtDecoder;

  @Test
  void contextLoads() {
  }

  @Test
  void mainShouldRunApplication() {
    try (MockedStatic<SpringApplication> springApplicationMock =
        mockStatic(SpringApplication.class)) {
      springApplicationMock
          .when(() -> SpringApplication.run(BeForumApplication.class, new String[] {}))
          .thenReturn(mock(ConfigurableApplicationContext.class));

      BeForumApplication.main(new String[] {});

      springApplicationMock.verify(
          () -> SpringApplication.run(BeForumApplication.class, new String[] {}));
    }
  }
}
