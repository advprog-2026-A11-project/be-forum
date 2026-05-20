package id.ac.ui.cs.advprog.beforum.service;

import id.ac.ui.cs.advprog.beforum.model.Message;
import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheInvalidationService {

  private static final String LIST_DTO_CACHE = "messages:list:dto";
  private static final String DETAIL_DTO_CACHE = "messages:detail:dto";
  private static final String REPLIES_DTO_CACHE = "messages:replies:dto";

  private final CacheManager cacheManager;

  public CacheInvalidationService(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  public void evictListByReadingId(String readingId) {
    Cache cache = cacheManager.getCache(LIST_DTO_CACHE);
    if (cache == null) {
      return;
    }
    String key = (readingId == null || readingId.isBlank()) ? "ALL" : readingId.trim();
    cache.evict(key);
  }

  public void evictDetail(UUID messageId) {
    if (messageId == null) {
      return;
    }
    Cache cache = cacheManager.getCache(DETAIL_DTO_CACHE);
    if (cache != null) {
      cache.evict(messageId);
    }
  }

  public void evictReplies(UUID parentId) {
    if (parentId == null) {
      return;
    }
    Cache cache = cacheManager.getCache(REPLIES_DTO_CACHE);
    if (cache != null) {
      cache.evict(parentId);
    }
  }

  public void evictForMessageMutation(Message message) {
    if (message == null) {
      return;
    }

    evictDetail(message.getId());
    evictListByReadingId(message.getReadingId());

    UUID parentId = message.getParentId();
    if (parentId != null) {
      evictReplies(parentId);
      evictDetail(parentId);
    }
  }

  public void evictForNewMessage(Message created) {
    if (created == null) {
      return;
    }
    evictForMessageMutation(created);
  }
}
