package com.example.kserverproject.domain.order.service;

import com.example.kserverproject.common.config.redis.RedisLockService;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import com.example.kserverproject.domain.order.dto.event.OrderEventDto;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.entity.OrderItem;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.order.producer.OrderProducer;
import com.example.kserverproject.domain.order.repository.OrderRepository;
import com.example.kserverproject.domain.order.util.OrderItemFactory;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final OrderProducer orderProducer;
    private final RedisLockService redisLockService;
    private final OrderItemFactory orderItemFactory;

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

            orderRepository.save(order);

            // Kafka 이벤트 발생
            orderProducer.send(OrderEventDto.from(order));

            return CreateOrderResponseDto.from(order);
        });

    }
}
