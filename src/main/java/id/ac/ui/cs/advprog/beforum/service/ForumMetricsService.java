package id.ac.ui.cs.advprog.beforum.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ForumMetricsService {

  private final Counter createdCounter;
  private final Counter updatedCounter;
  private final Counter deletedCounter;
  private final Counter fetchedCounter;
  private final MeterRegistry meterRegistry;

  public ForumMetricsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.createdCounter = Counter.builder("forum_messages_created_total")
        .description("Total forum messages created")
        .register(meterRegistry);
    this.updatedCounter = Counter.builder("forum_messages_updated_total")
        .description("Total forum messages updated")
        .register(meterRegistry);
    this.deletedCounter = Counter.builder("forum_messages_deleted_total")
        .description("Total forum messages deleted")
        .register(meterRegistry);
    this.fetchedCounter = Counter.builder("forum_messages_fetched_total")
        .description("Total forum message fetch operations")
        .register(meterRegistry);
  }

  public void incrementCreated() {
    createdCounter.increment();
  }

  public void incrementUpdated() {
    updatedCounter.increment();
  }

  public void incrementDeleted() {
    deletedCounter.increment();
  }

  public void incrementFetched() {
    fetchedCounter.increment();
  }

  public void incrementDbOperation(String operation, String repositoryMethod) {
    Counter.builder("forum_db_operations_total")
        .description("Total forum database operations by operation and repository method")
        .tag("operation", operation)
        .tag("repository_method", repositoryMethod)
        .register(meterRegistry)
        .increment();
  }

  public void incrementCacheAccess(String result, String cacheName) {
    Counter.builder("forum_cache_access_total")
        .description("Total forum cache accesses by result and cache name")
        .tag("result", result)
        .tag("cache_name", cacheName)
        .register(meterRegistry)
        .increment();
  }
}
