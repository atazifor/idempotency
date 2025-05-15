package com.example.idempotency.repository;

import com.example.idempotency.model.TopUpAuditEntry;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface TopUpAuditRepository extends ReactiveMongoRepository<TopUpAuditEntry, String> {
    Flux<TopUpAuditEntry> findByUserIdOrderByTimestampDesc(String userId);
    Flux<TopUpAuditEntry> findByTimestampAfter(Instant from);
}
