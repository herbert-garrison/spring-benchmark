package com.benchmark.webmvc.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.benchmark.webmvc.domain.OrderInfo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderInfoResponseDto {
    private long id;
    private String userId;
    private String productId;
    private BigDecimal amount;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static OrderInfoResponseDto fromOrderInfo(OrderInfo data) {
        return OrderInfoResponseDto.builder()
            .id(data.getId())
            .userId(data.getUserId())
            .productId(data.getProductId())
            .amount(data.getAmount())
            .status(data.getStatus())
            .createdAt(data.getCreatedAt())
            .updatedAt(data.getUpdatedAt())
            .build();
    }
}
