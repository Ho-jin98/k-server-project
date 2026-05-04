package com.example.kserverproject.domain.menu.controller;

import com.example.kserverproject.common.dto.response.ApiResponse;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.menu.dto.request.CreateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.request.UpdateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.response.CreateMenuResponseDto;
import com.example.kserverproject.domain.menu.dto.response.UpdateMenuResponseDto;
import com.example.kserverproject.domain.menu.service.AdminMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    // 메뉴 생성 (관리자 권한)
    @PostMapping
    public ResponseEntity<ApiResponse<CreateMenuResponseDto>> createMenu(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody CreateMenuRequestDto requestDto) {

        Long adminId = customUserDetails.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(adminMenuService.createMenu(adminId, requestDto)));
    }

    // 메뉴 수정
    @PutMapping("/{menuId}")
    public ResponseEntity<ApiResponse<UpdateMenuResponseDto>> updateMenu(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateMenuRequestDto requestDto,
            @PathVariable Long menuId) {

        Long adminId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.of(adminMenuService.updateMenu(adminId, requestDto, menuId)));
    }

    // 메뉴 삭제
    @DeleteMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long menuId) {

        Long adminId = customUserDetails.getUser().getId();
        adminMenuService.deleteMenu(adminId, menuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.empty());
    }
}
