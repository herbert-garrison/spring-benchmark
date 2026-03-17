package com.benchmark.webmvc.services.orders;

import java.util.List;

import com.benchmark.webmvc.domain.CreateOrderOptions;
import com.benchmark.webmvc.domain.OrderInfo;

public interface OrderManager {
    OrderInfo createOrder(CreateOrderOptions options);

    OrderInfo getOrder(long id);

    List<OrderInfo> getUserOrders(String userId);

    OrderInfo processOrderWithMultipleIo(CreateOrderOptions options);
}
