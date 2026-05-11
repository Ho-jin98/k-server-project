package com.example.kserverproject.domain.order.service;

import com.example.kserverproject.common.exception.OrderException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.menu.service.MenuRedisService;
import com.example.kserverproject.domain.order.dto.event.OrderEventDto;
import com.example.kserverproject.domain.order.entity.Order;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaService {

    private final OrderRepository orderRepository;
    private final MenuRedisService menuRedisService;

    // DB 작업만 트랜잭션 안에서
    @Transactional
    public void processOrderCompleted(OrderEventDto orderEventDto) {

        // Order 상태 CREATED -> COMPLETE로 변경
        Order findOrder = orderRepository.findById(orderEventDto.orderId())
                .orElseThrow( () -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        // 이미 처리된 주문이면 스킵
        if (findOrder.getOrderStatus() == OrderStatus.COMPLETED) {
            log.info("이미 처리된 주문 - orderId: {}", orderEventDto.orderId());
            return;
        }

        findOrder.completeOrder();
    }

    // Redis는 트랜잭션 밖으로 분리
    public void updatePopularMenus(OrderEventDto orderEventDto) {

        String userId = orderEventDto.userId().toString();

        for (OrderEventDto.OrderItemEventDto item : orderEventDto.items()) {
            menuRedisService.incrementMenuScoreByOrderCreate(item.menuId(), userId);
        }
    }
}
