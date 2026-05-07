package com.example.kserverproject.domain.order.entity;

import com.example.kserverproject.common.exception.OrderException;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order 엔티티 테스트")
class OrderTest {

    private User customer;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.createCustomer1();
        order = TestFixtures.createCreatedOrder(customer); // CREATED 상태
    }

    @Nested
    @DisplayName("주문 완료 처리")
    class CompleteOrder {

        @Test
        @DisplayName("CREATED 상태에서 완료 처리하면 COMPLETED가 된다")
        void completeOrder_fromCreated_success() {
            order.completeOrder();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 완료 처리하면 OrderException이 발생한다")
        void completeOrder_fromCompleted_throwsOrderException() {
            order.completeOrder();

            assertThatThrownBy(() -> order.completeOrder())
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 완료 처리하면 OrderException이 발생한다")
        void completeOrder_fromCanceled_throwsOrderException() {
            order.completeOrder();
            order.cancelOrder();

            assertThatThrownBy(() -> order.completeOrder())
                    .isInstanceOf(OrderException.class);
        }
    }

    @Nested
    @DisplayName("주문 취소 처리")
    class CancelOrder {

        @Test
        @DisplayName("COMPLETED 상태에서 취소하면 CANCELED가 된다")
        void cancelOrder_fromCompleted_success() {
            order.completeOrder();
            order.cancelOrder();

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("CREATED 상태에서 취소하면 OrderException이 발생한다")
        void cancelOrder_fromCreated_throwsOrderException() {
            assertThatThrownBy(() -> order.cancelOrder())
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("이미 CANCELED된 주문을 다시 취소하면 OrderException이 발생한다")
        void cancelOrder_alreadyCanceled_throwsOrderException() {
            order.completeOrder();
            order.cancelOrder();

            assertThatThrownBy(() -> order.cancelOrder())
                    .isInstanceOf(OrderException.class);
        }
    }

    @Nested
    @DisplayName("주문 상태 전이 흐름")
    class OrderStatusFlow {

        @Test
        @DisplayName("CREATED → COMPLETED → CANCELED 정상 흐름이 가능하다")
        void orderStatus_fullFlow_success() {
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);

            order.completeOrder();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);

            order.cancelOrder();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
        }

        @Test
        @DisplayName("CREATED에서 바로 CANCELED는 불가능하다")
        void orderStatus_createdToCanceled_throwsOrderException() {
            assertThatThrownBy(() -> order.cancelOrder())
                    .isInstanceOf(OrderException.class);
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        }
    }
}