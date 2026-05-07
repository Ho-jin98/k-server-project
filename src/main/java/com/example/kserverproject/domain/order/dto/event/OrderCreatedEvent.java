package com.example.kserverproject.domain.order.dto.event;

import com.example.kserverproject.domain.order.entity.Order;

public record OrderCreatedEvent (OrderEventDto payload) {

    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(OrderEventDto.from(order));
    }
}
