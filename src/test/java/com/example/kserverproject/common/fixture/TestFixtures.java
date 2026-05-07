package com.example.kserverproject.common.fixture;

import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.enums.UserRole;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * TestFixtures -> 테스트에서 반복적으로 사용되는 더미데이터를 생성하는 헬퍼 클래스
 * DataInitializer와 동일한 더미데이터 기반 테스트 픽스처
 * 모든 테스트에서 일관된 객체를 사용하기 위해 중앙화
 */
public class TestFixtures {

    // ======================== User ========================

    public static User createAdmin() {
        User user = User.builder()
                .email("admin@test.com")
                .password("encodedPassword")
                .nickname("관리자")
                .role(UserRole.ADMIN)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    public static User createCustomer1() {
        User user = User.builder()
                .email("user1@test.com")
                .password("encodedPassword")
                .nickname("테스터1")
                .role(UserRole.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(user, "id", 2L);
        user.chargePoint(1_000_000L);
        return user;
    }

    public static User createCustomer2() {
        User user = User.builder()
                .email("user2@test.com")
                .password("encodedPassword")
                .nickname("테스터2")
                .role(UserRole.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);
        user.chargePoint(1_000_000L);
        return user;
    }

    public static User createCustomerWithNoPoint() {
        User user = User.builder()
                .email("poor@test.com")
                .password("encodedPassword")
                .nickname("잔액없음")
                .role(UserRole.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(user, "id", 99L);
        return user;
    }

    // ======================== Menu ========================

    public static Menu createAmericano() {
        Menu menu = Menu.builder()
                .menuName("아메리카노")
                .price(3000L)
                .imageUrl("https://example.com/images/americano.jpg")
                .build();
        ReflectionTestUtils.setField(menu, "id", 1L);
        return menu;
    }

    public static Menu createLatte() {
        Menu menu = Menu.builder()
                .menuName("카페라떼")
                .price(4000L)
                .imageUrl("https://example.com/images/caffe-latte.jpg")
                .build();
        ReflectionTestUtils.setField(menu, "id", 2L);
        return menu;
    }

    public static Menu createCappuccino() {
        Menu menu = Menu.builder()
                .menuName("카푸치노")
                .price(4500L)
                .imageUrl("https://example.com/images/cappuccino.jpg")
                .build();
        ReflectionTestUtils.setField(menu, "id", 3L);
        return menu;
    }

    // ======================== Order ========================

    public static Order createCreatedOrder(User user) {
        Order order = Order.builder()
                .user(user)
                .totalAmount(6000L)
                .orderStatus(OrderStatus.CREATED)
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        return order;
    }

    public static Order createCompletedOrder(User user) {
        Order order = Order.builder()
                .user(user)
                .totalAmount(6000L)
                .orderStatus(OrderStatus.CREATED)
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        order.completeOrder();
        return order;
    }
}