package com.benchmark.webmvc.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.benchmark.webmvc.entities.OrderEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Query(
        value = "SELECT * FROM orders WHERE user_id = :userId",
        nativeQuery = true
    )
    List<OrderEntity> findByUserId(@Param("userId") String userId);

    @Query(
        value = "SELECT * FROM orders WHERE user_id = :userId ORDER BY created_at DESC",
        nativeQuery = true
    )
    List<OrderEntity> findRecentByUserId(@Param("userId") String userId);
}
