package com.example.kserverproject.domain.order.controller;

import com.example.kserverproject.common.dto.response.ApiResponse;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderCancelResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderDetailResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderListResponseDto;
import com.example.kserverproject.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

    // 내 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getOrderDetail(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long orderId) {

        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.of(orderService.getOrderDetail(userId, orderId)));
    }

    // 내 주문 목록 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponseDto<OrderListResponseDto>>> getOrderList(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = customUserDetails.getUser().getId();
        Pageable pageable = PageRequest.of(page - 1, size);
        return ResponseEntity.ok(ApiResponse.of(orderService.getOrderList(userId, pageable)));
    }

    // 주문 취소
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderCancelResponseDto>> cancelMyOrder(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long orderId) {

        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(orderService.cancelMyOrder(userId, orderId)));
    }
}
