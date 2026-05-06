package com.example.kserverproject.domain.order.dto.response;

import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.enums.OrderStatus;

public record OrderCancelResponseDto (

        Long orderId,
        OrderStatus orderStatus,
        Long refundedAmount,
        Long pointBalance
) {
    public static OrderCancelResponseDto of(Order order, Long refundedAmount, Long pointBalance) {
        return new OrderCancelResponseDto(
                order.getId(),
                order.getOrderStatus(),
                refundedAmount, // 환불 금액 -> 전액 환불!! (전액 = totalAmount)
                pointBalance // 환불 후 유저의 포인트 잔액, (User에서 가져오면 됨!!)
        );
    }
}
