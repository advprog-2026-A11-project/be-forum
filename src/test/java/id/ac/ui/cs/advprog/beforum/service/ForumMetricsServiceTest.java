package id.ac.ui.cs.advprog.beforum.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ForumMetricsServiceTest {

  @Test
  void incrementMessageCountersShouldIncreaseRegisteredMeters() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ForumMetricsService service = new ForumMetricsService(registry);

    service.incrementCreated();
    service.incrementUpdated();
    service.incrementDeleted();
    service.incrementFetched();

    assertEquals(1.0, registry.counter("forum_messages_created_total").count());
    assertEquals(1.0, registry.counter("forum_messages_updated_total").count());
    assertEquals(1.0, registry.counter("forum_messages_deleted_total").count());
    assertEquals(1.0, registry.counter("forum_messages_fetched_total").count());
  }

  @Test
  void incrementDbOperationShouldCreateAndIncrementTaggedCounter() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ForumMetricsService service = new ForumMetricsService(registry);

    service.incrementDbOperation("read", "MessageRepository.findById");
    service.incrementDbOperation("read", "MessageRepository.findById");

    Counter counter = registry.find("forum_db_operations_total")
        .tag("operation", "read")
        .tag("repository_method", "MessageRepository.findById")
        .counter();

    assertNotNull(counter);
    assertEquals(2.0, counter.count());
  }

  @Test
  void incrementCacheAccessShouldCreateAndIncrementTaggedCounter() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ForumMetricsService service = new ForumMetricsService(registry);

    service.incrementCacheAccess("hit", "messages:list:dto");

    Counter counter = registry.find("forum_cache_access_total")
        .tag("result", "hit")
        .tag("cache_name", "messages:list:dto")
        .counter();

    assertNotNull(counter);
    assertEquals(1.0, counter.count());
  }
}
