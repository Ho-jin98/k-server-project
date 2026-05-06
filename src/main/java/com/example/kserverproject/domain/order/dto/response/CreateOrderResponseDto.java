package com.example.kserverproject.domain.order.dto.response;

import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CreateOrderResponseDto(

        Long orderId,
        Long userId,
        List<OrderItemResponseDto> orderItems,
        Long totalAmount,
        OrderStatus orderStatus,
        LocalDateTime createdAt
) {
    public record OrderItemResponseDto(
            Long menuId,
            String menuName,
            int quantity,
            Long price
    ) {}

    public static CreateOrderResponseDto from(Order order) {
        return new CreateOrderResponseDto(
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
