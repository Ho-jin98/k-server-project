package com.example.kserverproject.domain.pointHistory.controller;

import com.example.kserverproject.common.dto.response.ApiResponse;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.pointHistory.dto.request.PointChargeRequestDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointBalanceResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointChargeResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointHistoryTransactionalResponseDto;
import com.example.kserverproject.domain.pointHistory.service.PointHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points/me")
@RequiredArgsConstructor
public class PointHistoryController {

    private final PointHistoryService pointHistoryService;

    // 내 포인트 잔액 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PointBalanceResponseDto>> getMyPointBalance(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.of(pointHistoryService.getMyPointBalance(userId)));
    }

    // 포인트 충전
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PointChargeResponseDto>> chargePoint(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody PointChargeRequestDto requestDto) {

        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.of(pointHistoryService.chargePoint(userId, requestDto)));
    }

    // 포인트 거래 목록 조회
    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<PageResponseDto<PointHistoryTransactionalResponseDto>>> getMyPointTransactionalRecord(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page -1, size);

        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.of(pointHistoryService.getMyPointTransactionalRecord(userId, pageable)));
    }
}
