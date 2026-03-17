package com.benchmark.webflux.services.orders;

import java.time.OffsetDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benchmark.webflux.domain.CreateOrderOptions;
import com.benchmark.webflux.domain.OrderInfo;
import com.benchmark.webflux.dto.OrderEventMessage;
import com.benchmark.webflux.entities.OrderEntity;
import com.benchmark.webflux.repositories.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultOrderManager implements OrderManager {
    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderEventMessage> kafka;
    private static final String TOPIC = "orders";

    @Transactional
    @Override
    public Mono<OrderInfo> createOrder(CreateOrderOptions options) {
        OrderEntity entity = OrderEntity.builder()
            .userId(options.getUserId())
            .productId(options.getProductId())
            .amount(options.getAmount())
            .status("PENDING")
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

        return this.repository.save(entity)
            .flatMap(order -> this.sendEvent(order).thenReturn(order))
            .map(OrderInfo::fromOrderEntity);
    }

    @Override
    public Mono<OrderInfo> getOrder(long id) {
        return this.repository.findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException(("Order not found: " + id))))
            .map(OrderInfo::fromOrderEntity);
    }

    @Override
    public Flux<OrderInfo> getUserOrders(String userId) {
        return this.repository.findRecentByUserId(userId)
            .map(OrderInfo::fromOrderEntity);
    }

    @Override
    public Mono<OrderInfo> processOrderWithMultipleIo(CreateOrderOptions options) {
        return this.repository.findByUserId(options.getUserId())
            .count()
            .doOnNext(count ->
                log.debug("User {} has {} orders", options.getUserId(), count)
            )
            .flatMap(count -> this.repository.save(
                OrderEntity.builder()
                    .userId(options.getUserId())
                    .productId(options.getProductId())
                    .amount(options.getAmount())
                    .status("PENDING")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build()
            ))
            .flatMap(entity -> this.sendEvent(entity).thenReturn(entity))
            .map(OrderInfo::fromOrderEntity);
    }

    private Mono<Void> sendEvent(OrderEntity order) {
        return Mono.fromFuture(
            this.kafka.send(TOPIC, order.getId().toString(), OrderEventMessage.created(order)).toCompletableFuture()
        )
        .doOnError(ex -> log.error("Kafka failed for order {}", order.getId(), ex))
        .then();
    }
}
