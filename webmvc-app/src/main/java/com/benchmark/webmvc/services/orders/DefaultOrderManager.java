package com.benchmark.webmvc.services.orders;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benchmark.webmvc.domain.CreateOrderOptions;
import com.benchmark.webmvc.domain.OrderInfo;
import com.benchmark.webmvc.dto.OrderEventMessage;
import com.benchmark.webmvc.entities.OrderEntity;
import com.benchmark.webmvc.repositories.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultOrderManager implements OrderManager {
    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderEventMessage> kafka;
    private static final String TOPIC = "orders";

    @Transactional
    @Override
    public OrderInfo createOrder(CreateOrderOptions options) {
        OrderEntity entity = OrderEntity.builder()
            .userId(options.getUserId())
            .productId(options.getProductId())
            .amount(options.getAmount())
            .status("PENDING")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        // VT is parking at INSERT -- carrier thread is free
        OrderEntity insertedEntity = this.repository.save(entity);

        // VT is parking at Kafka message sending
        this.kafka.send(TOPIC, insertedEntity.getId().toString(), OrderEventMessage.created(insertedEntity))
            .whenComplete((r, ex) -> {
                if (ex != null) {
                    log.error("Kafka failed for order {}", insertedEntity.getId(), ex);
                }
            });

        return OrderInfo.fromOrderEntity(insertedEntity);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderInfo getOrder(long id) {
        // VT is parking at SELECT
        OrderEntity entity = this.repository.findById(id).orElseThrow(() -> new RuntimeException("Order not found: " + id));

        return OrderInfo.fromOrderEntity(entity);
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderInfo> getUserOrders(String userId) {
        // VT is parking at SELECT
        List<OrderEntity> entities = this.repository.findRecentByUserId(userId);

        return entities.stream()
            .map(OrderInfo::fromOrderEntity)
            .toList();
    }

    // Extended case: a sequence of I/O operations
    @Transactional
    @Override
    public OrderInfo processOrderWithMultipleIo(CreateOrderOptions options) {
        // VT is parking at SELECT -- carrier thread is free
        List<OrderEntity> existing = this.repository.findByUserId(options.getUserId());
        log.debug("User {} has {} orders", options.getUserId(), existing.size());

        OrderEntity entity = OrderEntity.builder()
            .userId(options.getUserId())
            .productId(options.getProductId())
            .amount(options.getAmount())
            .status("PENDING")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        OrderEntity insertedEntity = this.repository.save(entity);

        // VT is parking at Kafka message sending
        this.kafka.send(TOPIC, insertedEntity.getId().toString(), OrderEventMessage.created(entity));

        return OrderInfo.fromOrderEntity(insertedEntity);
    }
}
