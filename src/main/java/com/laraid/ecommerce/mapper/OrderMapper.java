package com.laraid.ecommerce.mapper;

import com.laraid.ecommerce.domain.Order;
import com.laraid.ecommerce.dto.OrderDto;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public static OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        return OrderDto.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getAmount())
                .orderDate(order.getOrderDate())
                .build();
    }

    public static Order toEntity(OrderDto dto) {
        if (dto == null) {
            return null;
        }
        return Order.builder()
                .orderId(dto.getOrderId())
                .customerId(dto.getCustomerId())
                .amount(dto.getAmount())
                .orderDate(dto.getOrderDate())
                .build();
    }
}

