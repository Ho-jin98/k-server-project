package com.example.kserverproject.domain.order.controller;

import com.example.kserverproject.common.dto.response.ApiResponse;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponseDto>> createOrder(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody CreateOrderRequestDto requestDto) {

        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.of(orderService.createOrder(userId, requestDto)));
    }
}
