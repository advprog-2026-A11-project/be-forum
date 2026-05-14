package id.ac.ui.cs.advprog.beforum.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.beforum.controller.support.MessageResponseMapper;
import id.ac.ui.cs.advprog.beforum.dto.MessageResponse;
import id.ac.ui.cs.advprog.beforum.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import redis.embedded.RedisServer;

@SpringJUnitConfig(MessageCacheServiceTest.TestConfig.class)
@TestPropertySource(properties = {
    "spring.data.redis.host=127.0.0.1",
    "spring.data.redis.port=16379"
})
class MessageCacheServiceTest {

  @Configuration
  @EnableCaching
  static class TestConfig {

    private final int redisPort;
    private RedisServer redisServer;

    TestConfig(@Value("${spring.data.redis.port}") int redisPort) {
      this.redisPort = redisPort;
    }

    @PostConstruct
    void postConstruct() throws IOException {
      redisServer = new RedisServer(redisPort);
      redisServer.start();
    }

    @PreDestroy
    void preDestroy() throws IOException {
      if (redisServer != null) {
        redisServer.stop();
      }
    }

    @Bean
    LettuceConnectionFactory redisConnectionFactory(
        @Value("${spring.data.redis.host}") String host,
        @Value("${spring.data.redis.port}") int port) {
      RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
      return new LettuceConnectionFactory(config);
    }

    @Bean
    CacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
          .serializeValuesWith(
              RedisSerializationContext.SerializationPair.fromSerializer(
                  new GenericJackson2JsonRedisSerializer(mapper)))
          .disableCachingNullValues();
      return RedisCacheManager.builder(connectionFactory)
          .cacheDefaults(config)
          .build();
    }

    @Bean
    MessageService messageService() {
      return org.mockito.Mockito.mock(MessageService.class);
    }

    @Bean
    MessageResponseMapper messageResponseMapper() {
      return org.mockito.Mockito.mock(MessageResponseMapper.class);
    }

    @Bean
    MessageQueryCacheService messageQueryCacheService(
        MessageService messageService,
        MessageResponseMapper mapper) {
      return new MessageQueryCacheService(messageService, mapper);
    }

    @Bean
    CacheInvalidationService cacheInvalidationService(CacheManager cacheManager) {
      return new CacheInvalidationService(cacheManager);
    }
  }

  @Autowired
  private MessageService messageService;

  @Autowired
  private MessageResponseMapper responseMapper;

  @Autowired
  private MessageQueryCacheService queryCacheService;

  @Autowired
  private CacheInvalidationService cacheInvalidationService;

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    reset(messageService, responseMapper);
    clearCache("messages:list:dto");
    clearCache("messages:detail:dto");
    clearCache("messages:replies:dto");
  }

  @Test
  void listMessagesDtoShouldUseCacheOnSecondCall() {
    Message message = message(UUID.randomUUID(), "reading-1", null);
    MessageResponse response = response(message.getId());

    when(messageService.listMessages("reading-1")).thenReturn(List.of(message));
    when(messageService.getReplyCountsByParentIds(List.of(message.getId())))
        .thenReturn(Map.of(message.getId(), 2L));
    when(responseMapper.toResponsesShallow(List.of(message), Map.of(message.getId(), 2L)))
        .thenReturn(List.of(response));

    queryCacheService.listMessagesDto("reading-1");
    queryCacheService.listMessagesDto("reading-1");
    verify(messageService, times(1)).listMessages("reading-1");
    verify(messageService, times(1)).getReplyCountsByParentIds(List.of(message.getId()));
    verify(responseMapper, times(1))
        .toResponsesShallow(List.of(message), Map.of(message.getId(), 2L));
  }

  @Test
  void evictListByReadingIdShouldForceReload() {
    Message message = message(UUID.randomUUID(), "reading-1", null);
    MessageResponse response = response(message.getId());

    when(messageService.listMessages("reading-1")).thenReturn(List.of(message));
    when(messageService.getReplyCountsByParentIds(any())).thenReturn(Map.of(message.getId(), 1L));
    when(responseMapper.toResponsesShallow(any(), any())).thenReturn(List.of(response));

    queryCacheService.listMessagesDto("reading-1");
    cacheInvalidationService.evictListByReadingId("reading-1");
    queryCacheService.listMessagesDto("reading-1");

    verify(messageService, times(2)).listMessages("reading-1");
  }

  @Test
  void evictForMessageMutationShouldEvictDetailAndParentReplies() {
    UUID messageId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    String readingId = "reading-1";

    Message parent = message(parentId, readingId, null);
    Message mutated = message(messageId, readingId, parent);
    MessageResponse detailResponse = response(messageId);
    MessageResponse replyResponse = response(UUID.randomUUID());

    when(messageService.findByIdWithReplies(messageId)).thenReturn(mutated);
    when(responseMapper.toResponse(mutated)).thenReturn(detailResponse);

    when(messageService.findById(parentId)).thenReturn(parent);
    when(messageService.getReplies(parentId)).thenReturn(List.of(mutated));
    when(responseMapper.toResponses(List.of(mutated))).thenReturn(List.of(replyResponse));

    queryCacheService.getMessageDetailDto(messageId);
    queryCacheService.getRepliesDto(parentId);

    cacheInvalidationService.evictForMessageMutation(mutated);

    queryCacheService.getMessageDetailDto(messageId);
    queryCacheService.getRepliesDto(parentId);

    verify(messageService, times(2)).findByIdWithReplies(messageId);
    verify(messageService, times(2)).findById(parentId);
    verify(messageService, times(2)).getReplies(parentId);
  }

  private void clearCache(String name) {
    Cache cache = cacheManager.getCache(name);
    if (cache != null) {
      cache.clear();
    }
  }

  private Message message(UUID id, String readingId, Message parent) {
    Message message = new Message();
    message.setId(id);
    message.setContent("content");
    message.setReadingId(readingId);
    message.setUserId(UUID.randomUUID());
    message.setCreatedAt(OffsetDateTime.now());
    message.setParent(parent);
    return message;
  }

  private MessageResponse response(UUID id) {
    return new MessageResponse(
        id,
        "content",
        OffsetDateTime.now(),
        "reading-1",
        UUID.randomUUID(),
        null,
        0L,
        List.of(),
        List.of(),
        Map.of());
  }


  @Test
  void getMessageDetailDtoShouldReturnNullWhenMessageMissing() {
    UUID id = UUID.randomUUID();
    when(messageService.findByIdWithReplies(id)).thenReturn(null);

    queryCacheService.getMessageDetailDto(id);

    verify(messageService, times(1)).findByIdWithReplies(id);
  }

  @Test
  void getRepliesDtoShouldReturnNullWhenParentMissing() {
    UUID parentId = UUID.randomUUID();
    when(messageService.findById(parentId)).thenReturn(null);

    queryCacheService.getRepliesDto(parentId);

    verify(messageService, times(1)).findById(parentId);
    verify(messageService, times(0)).getReplies(parentId);
  }

}
