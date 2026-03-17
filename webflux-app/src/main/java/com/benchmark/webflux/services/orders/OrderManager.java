package com.benchmark.webflux.services.orders;

import com.benchmark.webflux.domain.CreateOrderOptions;
import com.benchmark.webflux.domain.OrderInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderManager {
    Mono<OrderInfo> createOrder(CreateOrderOptions options);

    Mono<OrderInfo> getOrder(long id);

    Flux<OrderInfo> getUserOrders(String userId);

    Mono<OrderInfo> processOrderWithMultipleIo(CreateOrderOptions options);
}
