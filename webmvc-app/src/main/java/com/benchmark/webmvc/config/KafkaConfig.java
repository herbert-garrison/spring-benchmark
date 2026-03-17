package com.benchmark.webmvc.config;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.benchmark.webmvc.dto.OrderEventMessage;

@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, OrderEventMessage> kafkaTemplate() {
        Map<String, Object> props = Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class, // For Spring Boot 4 replace with JacksonJsonSerializer (Jackson3)
            ProducerConfig.LINGER_MS_CONFIG, 5, // Delay for load testing
            ProducerConfig.BATCH_SIZE_CONFIG, 16384,
            ProducerConfig.ACKS_CONFIG, "1"
        );

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
