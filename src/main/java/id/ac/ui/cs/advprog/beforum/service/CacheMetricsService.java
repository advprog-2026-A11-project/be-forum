package id.ac.ui.cs.advprog.beforum.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheMetricsService {

  private static final String LIST_DTO_CACHE = "messages:list:dto";
  private static final String DETAIL_DTO_CACHE = "messages:detail:dto";
  private static final String REPLIES_DTO_CACHE = "messages:replies:dto";

  private final CacheManager cacheManager;
  private final ForumMetricsService forumMetricsService;

  public CacheMetricsService(CacheManager cacheManager, ForumMetricsService forumMetricsService) {
    this.cacheManager = cacheManager;
    this.forumMetricsService = forumMetricsService;
  }

  public void recordListAccess(String readingId) {
    String key = (readingId == null || readingId.isBlank()) ? "ALL" : readingId.trim();
    recordAccess(LIST_DTO_CACHE, key);
  }

  public void recordDetailAccess(Object id) {
    recordAccess(DETAIL_DTO_CACHE, id);
  }

  public void recordRepliesAccess(Object parentId) {
    recordAccess(REPLIES_DTO_CACHE, parentId);
  }

  private void recordAccess(String cacheName, Object key) {
    Cache cache = cacheManager.getCache(cacheName);
    boolean hit = cache != null && cache.get(key) != null;
    forumMetricsService.incrementCacheAccess(hit ? "hit" : "miss", cacheName);
  }
}

