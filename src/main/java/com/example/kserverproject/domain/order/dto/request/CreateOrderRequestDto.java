package com.example.kserverproject.domain.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequestDto(

        @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
        List<OrderItemRequestDto> menuItems
) {
    public record OrderItemRequestDto (

            @NotNull(message = "메뉴 ID는 필수입니다.")
            Long menuId,

            @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
            int quantity
    ) {}
}
