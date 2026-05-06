package com.example.kserverproject.domain.order.dto.event;

import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.enums.OrderStatus;

import java.util.List;

public record OrderEventDto (

        Long userId,
        Long orderId,
        List<OrderItemEventDto> items,  // 여러 메뉴를 담을 수 있는 리스트 구조
        Long totalPrice,                // 데이터 분석용 총 결제 금액
        OrderStatus orderStatus,
        String createdAt
) {
    public record OrderItemEventDto(
            Long menuId,
            int quantity
    ) {}

    public static OrderEventDto from(Order order) {
        return new OrderEventDto(
                order.getUser().getId(),
                order.getId(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemEventDto(
                                item.getMenu().getId(),
                                item.getQuantity()
                        ))
                        .toList(),
                order.getTotalAmount(),
                order.getOrderStatus(),
                order.getCreatedAt().toString()
        );
    }
}
