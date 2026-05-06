package com.example.kserverproject.domain.order.util;

import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderItemFactory {

    // OrderItem 리스트 생성 + 총금액 계산
    public List<OrderItem> createOrderItems(
            List<CreateOrderRequestDto.OrderItemRequestDto> menuItems,
            List<Menu> menus) {

        Map<Long, Menu> menuMap = new HashMap<>();
        for (Menu menu : menus) {
            menuMap.put(menu.getId(), menu);
        }

        // OrderItem 리스트 생성
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderRequestDto.OrderItemRequestDto itemDto : menuItems) {
            Menu menu = menuMap.get(itemDto.menuId());

            OrderItem orderItem = OrderItem.builder()
                    .menu(menu)
                    .quantity(itemDto.quantity())
                    .price(menu.getPrice())
                    .build();
            orderItems.add(orderItem);
        }
        return orderItems;
    }


    // 총 금액 계산
    public long calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    /* .mapToLong(item -> item.getPrice() * item.getQuantity())
     -> 각 OrderItem을 "가격 * 수량" Long값으로 변환
     ex) 아메리카노 3000원 * 2개 = 6000원 (Long 타입)*/


    /* .sum();
     -> 변환된 Long 값들을 전부 더해라
     ex) 6000 * 4500 = 10500*/


    /*
    이것을 for문으로 풀어서 쓰면,

    long total = 0L;

    for (OrderItem item : orderItems) {
        total += item.getPrice() * item.getQuantity();
    }

    return total;

    이러한 로직임
     */
}
