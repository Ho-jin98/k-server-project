package com.example.kserverproject.domain.order.util;

import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.entity.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderItemFactory 테스트")
class OrderItemFactoryTest {

    private final OrderItemFactory orderItemFactory = new OrderItemFactory();

    @Test
    @DisplayName("중복된 menuId는 quantity를 합산하여 하나의 OrderItem을 생성한다")
    void createOrderItems_duplicateMenuId_mergesQuantity() {
        // given
        List<CreateOrderRequestDto.OrderItemRequestDto> menuItems = List.of(
                new CreateOrderRequestDto.OrderItemRequestDto(1L, 2),
                new CreateOrderRequestDto.OrderItemRequestDto(1L, 3),  // 중복
                new CreateOrderRequestDto.OrderItemRequestDto(2L, 1)
        );

        List<Menu> menus = List.of(
                TestFixtures.createAmericano(),  // id=1
                TestFixtures.createLatte()       // id=2
        );

        // when
        List<OrderItem> orderItems = orderItemFactory.createOrderItems(menuItems, menus);

        // then
        assertThat(orderItems).hasSize(2);  // 3개 요청 → 2개 OrderItem (중복 병합)

        // menuId=1인 OrderItem 찾기
        OrderItem americanoItem = orderItems.stream()
                .filter(item -> item.getMenu().getId().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(americanoItem.getQuantity()).isEqualTo(5);  // 2 + 3 = 5
        assertThat(americanoItem.getPrice()).isEqualTo(3000L);

        // menuId=2인 OrderItem 확인
        OrderItem latteItem = orderItems.stream()
                .filter(item -> item.getMenu().getId().equals(2L))
                .findFirst()
                .orElseThrow();

        assertThat(latteItem.getQuantity()).isEqualTo(1);
        assertThat(latteItem.getPrice()).isEqualTo(4000L);
    }

    @Test
    @DisplayName("중복이 없는 경우 요청한 개수만큼 OrderItem을 생성한다")
    void createOrderItems_noDuplicate_createsAllItems() {
        // given
        List<CreateOrderRequestDto.OrderItemRequestDto> menuItems = List.of(
                new CreateOrderRequestDto.OrderItemRequestDto(1L, 2),
                new CreateOrderRequestDto.OrderItemRequestDto(2L, 1),
                new CreateOrderRequestDto.OrderItemRequestDto(3L, 3)
        );

        List<Menu> menus = List.of(
                TestFixtures.createAmericano(),   // id=1
                TestFixtures.createLatte(),       // id=2
                TestFixtures.createCappuccino()   // id=3
        );

        // when
        List<OrderItem> orderItems = orderItemFactory.createOrderItems(menuItems, menus);

        // then
        assertThat(orderItems).hasSize(3);
    }

    @Test
    @DisplayName("총 금액을 정확히 계산한다")
    void calculateTotalAmount_success() {
        // given
        List<Menu> menus = List.of(
                TestFixtures.createAmericano(),  // 3000원
                TestFixtures.createLatte()       // 4000원
        );

        List<OrderItem> orderItems = List.of(
                OrderItem.builder()
                        .menu(menus.get(0))
                        .quantity(2)
                        .price(3000L)
                        .build(),
                OrderItem.builder()
                        .menu(menus.get(1))
                        .quantity(1)
                        .price(4000L)
                        .build()
        );

        // when
        long totalAmount = orderItemFactory.calculateTotalAmount(orderItems);

        // then
        assertThat(totalAmount).isEqualTo(10000L);  // 3000*2 + 4000*1 = 10000
    }
}