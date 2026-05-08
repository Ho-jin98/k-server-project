package com.example.kserverproject.domain.order.facade;

import com.example.kserverproject.common.config.redis.RedisLockService;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderFacade 테스트")
class OrderFacadeTest {

    @InjectMocks
    private OrderFacade orderFacade;

    @Mock
    private OrderService orderService;

    @Mock
    private RedisLockService redisLockService;

    @SuppressWarnings("unchecked")
    private void mockRedisLock() {
        given(redisLockService.executeWithLock(anyString(), any()))
                .willAnswer(invocation -> {
                    Supplier<Object> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
    }

    @Test
    @DisplayName("주문 생성 시 Redis 락 안에서 OrderService가 호출된다")
    void createOrder_executesInsideLock() {
        mockRedisLock();

        CreateOrderRequestDto request = new CreateOrderRequestDto(
                List.of(new CreateOrderRequestDto.OrderItemRequestDto(1L, 2))
        );
        CreateOrderResponseDto mockResponse = mock(CreateOrderResponseDto.class);

        given(orderService.createOrder(2L, request)).willReturn(mockResponse);

        CreateOrderResponseDto result = orderFacade.createOrder(2L, request);

        assertThat(result).isEqualTo(mockResponse);
        verify(redisLockService).executeWithLock(eq("lock:order:2"), any());
        verify(orderService).createOrder(2L, request);
    }
}
