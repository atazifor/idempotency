package com.example.idempotency.config;

import com.example.idempotency.model.TopUpAuditEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {
    @Bean
    public KafkaTemplate<String, TopUpAuditEntry> kafkaTemplate(ProducerFactory<String, TopUpAuditEntry> factory) {
        return new KafkaTemplate<>(factory);
    }
}
