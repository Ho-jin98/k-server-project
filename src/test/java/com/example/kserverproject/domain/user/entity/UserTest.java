package com.example.kserverproject.domain.user.entity;

import com.example.kserverproject.common.exception.PointException;
import com.example.kserverproject.common.fixture.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 엔티티 테스트")
class UserTest {

    private User customer;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.createCustomer1(); // 초기 잔액 1_000_000L
    }

    @Nested
    @DisplayName("포인트 충전")
    class ChargePoint {

        @Test
        @DisplayName("정상 충전 시 잔액이 증가한다")
        void chargePoint_success() {
            customer.chargePoint(5000L);
            assertThat(customer.getPointBalance()).isEqualTo(1_005_000L);
        }

        @Test
        @DisplayName("여러 번 충전하면 누적된다")
        void chargePoint_multiple_accumulates() {
            customer.chargePoint(3000L);
            customer.chargePoint(2000L);
            assertThat(customer.getPointBalance()).isEqualTo(1_005_000L);
        }

        @Test
        @DisplayName("충전 금액이 0이면 PointException이 발생한다")
        void chargePoint_zero_throwsPointException() {
            assertThatThrownBy(() -> customer.chargePoint(0L))
                    .isInstanceOf(PointException.class);
        }

        @Test
        @DisplayName("충전 금액이 음수이면 PointException이 발생한다")
        void chargePoint_negative_throwsPointException() {
            assertThatThrownBy(() -> customer.chargePoint(-1000L))
                    .isInstanceOf(PointException.class);
        }
    }

    @Nested
    @DisplayName("포인트 차감")
    class DeductPoint {

        @Test
        @DisplayName("잔액이 충분하면 차감된다")
        void deductPoint_success() {
            customer.deductPoint(3000L);
            assertThat(customer.getPointBalance()).isEqualTo(997_000L);
        }

        @Test
        @DisplayName("잔액 전액을 차감할 수 있다")
        void deductPoint_fullBalance() {
            customer.deductPoint(1_000_000L);
            assertThat(customer.getPointBalance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("잔액보다 많은 금액 차감 시 PointException이 발생한다")
        void deductPoint_exceedsBalance_throwsPointException() {
            assertThatThrownBy(() -> customer.deductPoint(2_000_000L))
                    .isInstanceOf(PointException.class);
        }

        @Test
        @DisplayName("잔액 차감 후 재차감 시 정합성이 유지된다")
        void deductPoint_sequential_isConsistent() {
            customer.deductPoint(600_000L); // 400_000 남음
            customer.deductPoint(400_000L); // 0 남음
            assertThat(customer.getPointBalance()).isEqualTo(0L);

            assertThatThrownBy(() -> customer.deductPoint(1L))
                    .isInstanceOf(PointException.class);
        }
    }

    @Nested
    @DisplayName("포인트 환불")
    class RefundPoint {

        @Test
        @DisplayName("정상 환불 시 잔액이 증가한다")
        void refundPoint_success() {
            customer.deductPoint(6000L);
            customer.refundPoint(6000L);
            assertThat(customer.getPointBalance()).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("환불 금액이 0이면 PointException이 발생한다")
        void refundPoint_zero_throwsPointException() {
            assertThatThrownBy(() -> customer.refundPoint(0L))
                    .isInstanceOf(PointException.class);
        }

        @Test
        @DisplayName("환불 금액이 음수이면 PointException이 발생한다")
        void refundPoint_negative_throwsPointException() {
            assertThatThrownBy(() -> customer.refundPoint(-500L))
                    .isInstanceOf(PointException.class);
        }
    }

    @Test
    @DisplayName("신규 유저의 초기 포인트 잔액은 0이다")
    void newUser_initialPointBalance_isZero() {
        User newUser = TestFixtures.createCustomerWithNoPoint();
        assertThat(newUser.getPointBalance()).isEqualTo(0L);
    }
}