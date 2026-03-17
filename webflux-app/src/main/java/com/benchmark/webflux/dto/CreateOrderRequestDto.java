package com.benchmark.webflux.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CreateOrderRequestDto {
    private String userId;
    private String productId;
    private BigDecimal amount;
}
