package id.ac.ui.cs.advprog.beforum.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class CacheMetricsServiceTest {

  @Test
  void recordListAccessShouldUseAllKeyForNullReadingIdAndRecordHit() {
    CacheManager cacheManager = mock(CacheManager.class);
    ForumMetricsService forumMetricsService = mock(ForumMetricsService.class);
    Cache cache = mock(Cache.class);
    Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);

    when(cacheManager.getCache("messages:list:dto")).thenReturn(cache);
    when(cache.get("ALL")).thenReturn(valueWrapper);

    CacheMetricsService service = new CacheMetricsService(cacheManager, forumMetricsService);
    service.recordListAccess(null);

    verify(cache).get("ALL");
    verify(forumMetricsService).incrementCacheAccess("hit", "messages:list:dto");
  }

  @Test
  void recordListAccessShouldUseAllKeyForBlankReadingId() {
    CacheManager cacheManager = mock(CacheManager.class);
    ForumMetricsService forumMetricsService = mock(ForumMetricsService.class);
    Cache cache = mock(Cache.class);

    when(cacheManager.getCache("messages:list:dto")).thenReturn(cache);
    when(cache.get("ALL")).thenReturn(null);

    CacheMetricsService service = new CacheMetricsService(cacheManager, forumMetricsService);
    service.recordListAccess("   ");

    verify(cache).get("ALL");
    verify(forumMetricsService).incrementCacheAccess("miss", "messages:list:dto");
  }

  @Test
  void recordListAccessShouldTrimReadingIdAndRecordMiss() {
    CacheManager cacheManager = mock(CacheManager.class);
    ForumMetricsService forumMetricsService = mock(ForumMetricsService.class);
    Cache cache = mock(Cache.class);

    when(cacheManager.getCache("messages:list:dto")).thenReturn(cache);
    when(cache.get("reading-1")).thenReturn(null);

    CacheMetricsService service = new CacheMetricsService(cacheManager, forumMetricsService);
    service.recordListAccess(" reading-1 ");

    verify(cache).get("reading-1");
    verify(forumMetricsService).incrementCacheAccess("miss", "messages:list:dto");
  }

  @Test
  void recordDetailAccessShouldRecordMissWhenCacheIsMissing() {
    CacheManager cacheManager = mock(CacheManager.class);
    ForumMetricsService forumMetricsService = mock(ForumMetricsService.class);
    UUID id = UUID.randomUUID();

    when(cacheManager.getCache("messages:detail:dto")).thenReturn(null);

    CacheMetricsService service = new CacheMetricsService(cacheManager, forumMetricsService);
    service.recordDetailAccess(id);

    verify(forumMetricsService).incrementCacheAccess("miss", "messages:detail:dto");
  }

  @Test
  void recordRepliesAccessShouldRecordHitWhenEntryExists() {
    CacheManager cacheManager = mock(CacheManager.class);
    ForumMetricsService forumMetricsService = mock(ForumMetricsService.class);
    Cache cache = mock(Cache.class);
    Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
    UUID parentId = UUID.randomUUID();

    when(cacheManager.getCache("messages:replies:dto")).thenReturn(cache);
    when(cache.get(parentId)).thenReturn(valueWrapper);

    CacheMetricsService service = new CacheMetricsService(cacheManager, forumMetricsService);
    service.recordRepliesAccess(parentId);

    verify(cache).get(parentId);
    verify(forumMetricsService).incrementCacheAccess("hit", "messages:replies:dto");
    verify(cacheManager, never()).getCache("messages:detail:dto");
  }
}
