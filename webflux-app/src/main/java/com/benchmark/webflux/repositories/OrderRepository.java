package com.benchmark.webflux.repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.benchmark.webflux.entities.OrderEntity;

import reactor.core.publisher.Flux;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<OrderEntity, Long> {
    @Query("SELECT * FROM orders WHERE user_id = :userId")
    Flux<OrderEntity> findByUserId(String userId);

    @Query("SELECT * FROM orders WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<OrderEntity> findRecentByUserId(String userId);
}
