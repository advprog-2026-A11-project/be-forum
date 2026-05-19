package id.ac.ui.cs.advprog.beforum.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class CacheInvalidationServiceTest {

  @Test
  void evictListByReadingIdShouldNoOpWhenCacheMissing() {
    CacheManager cacheManager = mock(CacheManager.class);
    when(cacheManager.getCache("messages:list:dto")).thenReturn(null);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictListByReadingId("reading-1");

    verify(cacheManager).getCache("messages:list:dto");
  }

  @Test
  void evictDetailShouldReturnWhenMessageIdIsNull() {
    CacheManager cacheManager = mock(CacheManager.class);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictDetail(null);

    verify(cacheManager, never()).getCache("messages:detail:dto");
  }

  @Test
  void evictRepliesShouldReturnWhenParentIdIsNull() {
    CacheManager cacheManager = mock(CacheManager.class);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictReplies(null);

    verify(cacheManager, never()).getCache("messages:replies:dto");
  }

  @Test
  void evictForMessageMutationShouldHandleNullMessage() {
    CacheManager cacheManager = mock(CacheManager.class);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictForMessageMutation(null);

    verify(cacheManager, never()).getCache("messages:list:dto");
  }

  @Test
  void evictForNewMessageShouldHandleNullAndNonNull() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache listCache = mock(Cache.class);
    Cache detailCache = mock(Cache.class);
    Cache repliesCache = mock(Cache.class);
    when(cacheManager.getCache("messages:list:dto")).thenReturn(listCache);
    when(cacheManager.getCache("messages:detail:dto")).thenReturn(detailCache);
    when(cacheManager.getCache("messages:replies:dto")).thenReturn(repliesCache);

    CacheInvalidationService service = new CacheInvalidationService(cacheManager);
    service.evictForNewMessage(null);

    Message message = new Message();
    UUID id = UUID.randomUUID();
    Message parent = new Message();
    UUID parentId = UUID.randomUUID();
    parent.setId(parentId);
    message.setId(id);
    message.setParent(parent);
    message.setReadingId("reading-1");

    service.evictForNewMessage(message);

    verify(listCache).evict("reading-1");
    verify(detailCache).evict(id);
    verify(repliesCache).evict(parentId);
    verify(detailCache).evict(parentId);
  }

  @Test
  void evictListByReadingIdShouldUseAllForBlankReadingId() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache listCache = mock(Cache.class);
    when(cacheManager.getCache("messages:list:dto")).thenReturn(listCache);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictListByReadingId("   ");

    verify(listCache).evict("ALL");
  }

  @Test
  void evictDetailShouldNoOpWhenDetailCacheMissing() {
    CacheManager cacheManager = mock(CacheManager.class);
    when(cacheManager.getCache("messages:detail:dto")).thenReturn(null);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictDetail(UUID.randomUUID());

    verify(cacheManager).getCache("messages:detail:dto");
  }

  @Test
  void evictRepliesShouldNoOpWhenRepliesCacheMissing() {
    CacheManager cacheManager = mock(CacheManager.class);
    when(cacheManager.getCache("messages:replies:dto")).thenReturn(null);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictReplies(UUID.randomUUID());

    verify(cacheManager).getCache("messages:replies:dto");
  }

  @Test
  void evictListByReadingIdShouldUseAllForNullReadingId() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache listCache = mock(Cache.class);
    when(cacheManager.getCache("messages:list:dto")).thenReturn(listCache);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    service.evictListByReadingId(null);

    verify(listCache).evict("ALL");
  }

  @Test
  void evictForMessageMutationShouldSkipParentEvictionWhenParentIsNull() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache listCache = mock(Cache.class);
    Cache detailCache = mock(Cache.class);
    Cache repliesCache = mock(Cache.class);
    when(cacheManager.getCache("messages:list:dto")).thenReturn(listCache);
    when(cacheManager.getCache("messages:detail:dto")).thenReturn(detailCache);
    when(cacheManager.getCache("messages:replies:dto")).thenReturn(repliesCache);
    CacheInvalidationService service = new CacheInvalidationService(cacheManager);

    Message message = new Message();
    UUID id = UUID.randomUUID();
    message.setId(id);
    message.setReadingId("reading-2");

    service.evictForMessageMutation(message);

    verify(detailCache).evict(id);
    verify(listCache).evict("reading-2");
    verify(repliesCache, never()).evict(any());
  }

}
