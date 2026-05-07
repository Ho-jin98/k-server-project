package com.example.kserverproject.domain.order.service;

import com.example.kserverproject.common.config.redis.RedisLockService;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.OrderException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import com.example.kserverproject.domain.order.dto.event.OrderEventDto;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderCancelResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderDetailResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderListResponseDto;
import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.entity.OrderItem;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.order.producer.OrderProducer;
import com.example.kserverproject.domain.order.repository.OrderRepository;
import com.example.kserverproject.domain.order.util.OrderItemFactory;
import com.example.kserverproject.domain.pointHistory.enums.PointType;
import com.example.kserverproject.domain.pointHistory.service.PointHistoryService;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final PointHistoryService pointHistoryService;
    private final OrderProducer orderProducer;
    private final RedisLockService redisLockService;
    private final OrderItemFactory orderItemFactory;

    // 주문 생성
    @Transactional
    public CreateOrderResponseDto createOrder(Long userId, CreateOrderRequestDto requestDto) {

        return redisLockService.executeWithLock("lock:order:" + userId, () -> {

            // 유저 조회
            User user = userRepository.findByUserIdWithLock(userId)
                    .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

            // 메뉴 리스트 조회
            List<Menu> menus = new ArrayList<>();
            for (CreateOrderRequestDto.OrderItemRequestDto itemDto : requestDto.menuItems()) {
                Menu menu = menuRepository.findById(itemDto.menuId())
                        .orElseThrow( () -> new MenuException(ErrorCode.MENU_NOT_FOUND));
                menus.add(menu);
            }

            // OrderItem 생성과 총 금액 계산을 orderItemFactory에게 넘기기
            List<OrderItem> orderItemList = orderItemFactory.createOrderItems(requestDto.menuItems(), menus);
            long totalAmount = orderItemFactory.calculateTotalAmount(orderItemList);

            // 포인트 잔액 확인 + 차감
            user.deductPoint(totalAmount);

            // 주문 생성
            Order order = Order.builder()
                    .user(user)
                    .totalAmount(totalAmount)
                    .orderStatus(OrderStatus.CREATED)
                    .build();

            // OrderItem 중간 테이블에 Order 연결
            for (OrderItem orderItem : orderItemList) {
                orderItem.connectOrder(order);
                order.addOrderItem(orderItem);
            }

            Order savedOrder = orderRepository.save(order);

            // Kafka 이벤트 발생
            orderProducer.send(OrderEventDto.from(savedOrder));

            return CreateOrderResponseDto.from(savedOrder);
        });

    }

    // 주문 상세 조회
    public OrderDetailResponseDto getOrderDetail(Long userId, Long orderId) {

        User findUser = userRepository.findById(userId)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        Order findOrder = orderRepository.findOrderWithItemsAndMenus(orderId)
                .orElseThrow( () -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        if (!findOrder.getUser().getId().equals(findUser.getId())) {
            throw new OrderException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return OrderDetailResponseDto.from(findOrder);
    }

    // 주문 목록 조회
    public PageResponseDto<OrderListResponseDto> getOrderList(Long userId, Pageable pageable) {

        User findUser = userRepository.findById(userId)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        Page<OrderListResponseDto> result = orderRepository.findOrdersWithItemsAndMenus(findUser.getId(), pageable)
                .map(OrderListResponseDto::from);

        return PageResponseDto.of(result);

    }

    // 주문 취소
    @Transactional
    public OrderCancelResponseDto cancelMyOrder(Long userId, Long orderId) {

        User findUser = userRepository.findByUserIdWithLock(userId)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        Order findOrder = orderRepository.findById(orderId)
                .orElseThrow( () -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        if (!findOrder.getUser().getId().equals(findUser.getId())) {
            throw new OrderException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 주문 취소로 상태 변경
        findOrder.cancelOrder();

        // 포인트 환불
        findUser.refundPoint(findOrder.getTotalAmount());

        // 포인트 내역 기록 저장을 위해 pointHistoryService로 넘기기
        pointHistoryService.record(findUser, findOrder.getTotalAmount(), PointType.REFUND);

        return OrderCancelResponseDto.of(findOrder, findOrder.getTotalAmount(), findUser.getPointBalance());
    }
}
