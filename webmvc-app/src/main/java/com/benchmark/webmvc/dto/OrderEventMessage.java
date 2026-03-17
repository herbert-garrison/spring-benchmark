package com.benchmark.webmvc.dto;

import java.time.OffsetDateTime;

import com.benchmark.webmvc.entities.OrderEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventMessage {
    private Long orderId;
    private String userId;
    private String type;
    private OffsetDateTime timestamp;

    public static OrderEventMessage created(OrderEntity order) {
        return OrderEventMessage.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .type("ORDER_CREATED")
            .timestamp(OffsetDateTime.now())
            .build();
    }
}
