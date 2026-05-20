package id.ac.ui.cs.advprog.beforum.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ForumMetricsService {

  private final Counter messagesCreatedCounter;
  private final Counter messagesUpdatedCounter;
  private final Counter messagesDeletedCounter;
  private final Counter messagesFetchedCounter;

  public ForumMetricsService(MeterRegistry meterRegistry) {
    this.messagesCreatedCounter = Counter.builder("forum_messages_created_total")
        .description("Total number of forum messages created")
        .register(meterRegistry);
    this.messagesUpdatedCounter = Counter.builder("forum_messages_updated_total")
        .description("Total number of forum messages updated")
        .register(meterRegistry);
    this.messagesDeletedCounter = Counter.builder("forum_messages_deleted_total")
        .description("Total number of forum messages deleted")
        .register(meterRegistry);
    this.messagesFetchedCounter = Counter.builder("forum_messages_fetched_total")
        .description("Total number of forum message fetch operations")
        .register(meterRegistry);
  }

  public void incrementCreated() {
    messagesCreatedCounter.increment();
  }

  public void incrementUpdated() {
    messagesUpdatedCounter.increment();
  }

  public void incrementDeleted() {
    messagesDeletedCounter.increment();
  }

  public void incrementFetched() {
    messagesFetchedCounter.increment();
  }
}
