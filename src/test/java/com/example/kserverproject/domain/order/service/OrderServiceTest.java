package com.example.kserverproject.domain.order.service;

import com.example.kserverproject.common.config.redis.RedisLockService;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.OrderException;
import com.example.kserverproject.common.exception.PointException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import com.example.kserverproject.domain.order.dto.event.OrderCreatedEvent;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderCancelResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderDetailResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderListResponseDto;
import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.order.producer.OrderProducer;
import com.example.kserverproject.domain.order.repository.OrderRepository;
import com.example.kserverproject.domain.order.util.OrderItemFactory;
import com.example.kserverproject.domain.pointHistory.enums.PointType;
import com.example.kserverproject.domain.pointHistory.service.PointHistoryService;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private PointHistoryService pointHistoryService;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private OrderItemFactory orderItemFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // RedisLockService의 executeWithLock이 실제 supplier를 실행하도록 설정
    @SuppressWarnings("unchecked")
    private void mockRedisLock() {
        given(redisLockService.executeWithLock(anyString(), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<CreateOrderResponseDto> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("정상 주문 생성 시 포인트가 차감되고 Kafka 이벤트가 발행된다")
        void createOrder_success() {
            mockRedisLock();

            User customer = TestFixtures.createCustomer1(); // 1_000_000L

            given(userRepository.findByUserIdWithLock(2L)).willReturn(Optional.of(customer));
            given(menuRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAmericano()));
            given(orderItemFactory.createOrderItems(any(), any())).willReturn(List.of());
            given(orderItemFactory.calculateTotalAmount(any())).willReturn(6000L);
            given(orderRepository.save(any(Order.class))).willAnswer(inv -> {
                Order savedOrder = inv.getArgument(0);
                ReflectionTestUtils.setField(savedOrder, "createdAt", LocalDateTime.now());
                ReflectionTestUtils.setField(savedOrder, "updatedAt", LocalDateTime.now());
                return savedOrder;
            });

            CreateOrderRequestDto request = new CreateOrderRequestDto(
                    List.of(new CreateOrderRequestDto.OrderItemRequestDto(1L, 2))
            );

            CreateOrderResponseDto response = orderService.createOrder(2L, request);

            assertThat(response.orderStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(response.totalAmount()).isEqualTo(6000L);
            assertThat(customer.getPointBalance()).isEqualTo(994_000L); // 1_000_000 - 6_000
            verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
        }

        @Test
        @DisplayName("포인트 잔액이 부족하면 PointException이 발생한다")
        void createOrder_insufficientPoints_throwsPointException() {
            mockRedisLock();

            User customerWithNoPoint = TestFixtures.createCustomerWithNoPoint(); // 0L

            given(userRepository.findByUserIdWithLock(99L)).willReturn(Optional.of(customerWithNoPoint));
            given(menuRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAmericano()));
            given(orderItemFactory.createOrderItems(any(), any())).willReturn(List.of());
            given(orderItemFactory.calculateTotalAmount(any())).willReturn(3000L);

            CreateOrderRequestDto request = new CreateOrderRequestDto(
                    List.of(new CreateOrderRequestDto.OrderItemRequestDto(1L, 1))
            );

            assertThatThrownBy(() -> orderService.createOrder(99L, request))
                    .isInstanceOf(PointException.class);
        }

        @Test
        @DisplayName("존재하지 않는 메뉴로 주문 시 MenuException이 발생한다")
        void createOrder_menuNotFound_throwsMenuException() {
            mockRedisLock();

            given(userRepository.findByUserIdWithLock(2L)).willReturn(Optional.of(TestFixtures.createCustomer1()));
            given(menuRepository.findById(999L)).willReturn(Optional.empty());

            CreateOrderRequestDto request = new CreateOrderRequestDto(
                    List.of(new CreateOrderRequestDto.OrderItemRequestDto(999L, 1))
            );

            assertThatThrownBy(() -> orderService.createOrder(2L, request))
                    .isInstanceOf(MenuException.class);
        }

        @Test
        @DisplayName("존재하지 않는 유저로 주문 시 UserException이 발생한다")
        void createOrder_userNotFound_throwsUserException() {
            mockRedisLock();

            given(userRepository.findByUserIdWithLock(999L)).willReturn(Optional.empty());

            CreateOrderRequestDto request = new CreateOrderRequestDto(
                    List.of(new CreateOrderRequestDto.OrderItemRequestDto(1L, 1))
            );

            assertThatThrownBy(() -> orderService.createOrder(999L, request))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("주문 상세 조회")
    class GetOrderDetail {

        @Test
        @DisplayName("본인의 주문 상세를 조회한다")
        void getOrderDetail_success() {
            User customer = TestFixtures.createCustomer1();
            Order order = TestFixtures.createCompletedOrder(customer);

            given(userRepository.findById(2L)).willReturn(Optional.of(customer));
            given(orderRepository.findOrderWithItemsAndMenus(1L)).willReturn(Optional.of(order));

            OrderDetailResponseDto response = orderService.getOrderDetail(2L, 1L);

            assertThat(response.totalAmount()).isEqualTo(6000L);
            assertThat(response.orderStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 OrderException이 발생한다")
        void getOrderDetail_orderNotFound_throwsOrderException() {
            given(userRepository.findById(2L)).willReturn(Optional.of(TestFixtures.createCustomer1()));
            given(orderRepository.findOrderWithItemsAndMenus(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderDetail(2L, 999L))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("타인의 주문 조회 시 OrderException이 발생한다")
        void getOrderDetail_accessDenied_throwsOrderException() {
            User customer1 = TestFixtures.createCustomer1(); // id=2
            User customer2 = TestFixtures.createCustomer2(); // id=3
            Order order = TestFixtures.createCompletedOrder(customer2); // customer2 소유

            given(userRepository.findById(2L)).willReturn(Optional.of(customer1));
            given(orderRepository.findOrderWithItemsAndMenus(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderDetail(2L, 1L))
                    .isInstanceOf(OrderException.class);
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class GetOrderList {

        @Test
        @DisplayName("내 주문 목록을 페이징하여 반환한다")
        void getOrderList_success() {
            User customer = TestFixtures.createCustomer1();
            PageRequest pageable = PageRequest.of(0, 10);

            given(userRepository.findById(2L)).willReturn(Optional.of(customer));
            given(orderRepository.findOrdersWithItemsAndMenus(2L, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponseDto<OrderListResponseDto> response = orderService.getOrderList(2L, pageable);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelMyOrder {

        @Test
        @DisplayName("COMPLETED 주문 취소 시 포인트가 환불되고 이력이 저장된다")
        void cancelMyOrder_success() {
            User customer = TestFixtures.createCustomer1(); // 1_000_000L
            Order order = TestFixtures.createCompletedOrder(customer); // totalAmount=6000

            given(userRepository.findByUserIdWithLock(2L)).willReturn(Optional.of(customer));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            OrderCancelResponseDto response = orderService.cancelMyOrder(2L, 1L);

            assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELED);
            assertThat(response.refundedAmount()).isEqualTo(6000L);
            assertThat(customer.getPointBalance()).isEqualTo(1_006_000L); // 1_000_000 + 6_000 환불
            verify(pointHistoryService).record(customer, 6000L, PointType.REFUND);
        }

        @Test
        @DisplayName("CREATED 상태 주문 취소 시 OrderException이 발생한다")
        void cancelMyOrder_createdOrder_throwsOrderException() {
            User customer = TestFixtures.createCustomer1();
            Order createdOrder = TestFixtures.createCreatedOrder(customer); // CREATED 상태

            given(userRepository.findByUserIdWithLock(2L)).willReturn(Optional.of(customer));
            given(orderRepository.findById(1L)).willReturn(Optional.of(createdOrder));

            assertThatThrownBy(() -> orderService.cancelMyOrder(2L, 1L))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("타인의 주문 취소 시 OrderException이 발생한다")
        void cancelMyOrder_accessDenied_throwsOrderException() {
            User customer1 = TestFixtures.createCustomer1(); // id=2
            User customer2 = TestFixtures.createCustomer2(); // id=3
            Order order = TestFixtures.createCompletedOrder(customer2); // customer2 소유

            given(userRepository.findByUserIdWithLock(2L)).willReturn(Optional.of(customer1));
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelMyOrder(2L, 1L))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("존재하지 않는 주문 취소 시 OrderException이 발생한다")
        void cancelMyOrder_orderNotFound_throwsOrderException() {
            given(userRepository.findByUserIdWithLock(2L)).willReturn(Optional.of(TestFixtures.createCustomer1()));
            given(orderRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelMyOrder(2L, 999L))
                    .isInstanceOf(OrderException.class);
        }
    }
}