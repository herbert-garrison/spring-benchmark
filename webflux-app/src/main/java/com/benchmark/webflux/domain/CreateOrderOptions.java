package com.benchmark.webflux.domain;

import java.math.BigDecimal;

import com.benchmark.webflux.dto.CreateOrderRequestDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateOrderOptions {
    private String userId;
    private String productId;
    private BigDecimal amount;

    public static CreateOrderOptions fromCreateOrderRequestDto(CreateOrderRequestDto dto) {
        return CreateOrderOptions.builder()
            .userId(dto.getUserId())
            .productId(dto.getProductId())
            .amount(dto.getAmount())
            .build();
    }
}
