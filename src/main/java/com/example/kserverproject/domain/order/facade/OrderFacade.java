package com.example.kserverproject.domain.order.facade;

import com.example.kserverproject.common.config.redis.RedisLockService;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final RedisLockService redisLockService;

    public CreateOrderResponseDto createOrder(Long userId, CreateOrderRequestDto requestDto) {
        return redisLockService.executeWithLock("lock:order:" + userId, () ->
                orderService.createOrder(userId, requestDto)
        );
    }
}
