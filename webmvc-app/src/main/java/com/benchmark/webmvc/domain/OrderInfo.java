package com.benchmark.webmvc.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.benchmark.webmvc.entities.OrderEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderInfo {
    private long id;
    private String userId;
    private String productId;
    private BigDecimal amount;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static OrderInfo fromOrderEntity(OrderEntity entity) {
        return OrderInfo.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .productId(entity.getProductId())
            .amount(entity.getAmount())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
