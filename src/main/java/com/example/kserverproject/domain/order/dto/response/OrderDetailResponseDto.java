package com.example.kserverproject.domain.order.dto.response;

import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponseDto (

        Long orderId,
        Long userId,
        List<OrderItemResponseDto> orderItems, // 주문한 메뉴 목록
        Long totalAmount,
        OrderStatus orderStatus,
        LocalDateTime createdAt
) {
    public record OrderItemResponseDto (
            Long menuId,
            String menuName,
            int quantity,
            Long price
    ) {}

    public static OrderDetailResponseDto from(Order order) {
        return new OrderDetailResponseDto(
                order.getId(),
                order.getUser().getId(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemResponseDto(
                                item.getMenu().getId(),
                                item.getMenu().getMenuName(),
                                item.getQuantity(),
                                item.getPrice()
                        ))
                        .toList(),
                order.getTotalAmount(),
                order.getOrderStatus(),
                order.getCreatedAt()
        );
    }
}
