package com.example.kserverproject.domain.menu.controller;

import com.example.kserverproject.common.dto.response.ApiResponse;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.menu.dto.request.MenuSearchRequestDto;
import com.example.kserverproject.domain.menu.dto.response.MenuDetailResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuListResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuSearchResponseDto;
import com.example.kserverproject.domain.menu.dto.response.PopularMenuResponseDto;
import com.example.kserverproject.domain.menu.service.MenuPopularService;
import com.example.kserverproject.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final MenuPopularService menuPopularService;

    // 메뉴 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuListResponseDto>>> getMenus(){

        return ResponseEntity.ok(ApiResponse.of(menuService.getMenus()));
    }

    // 메뉴 상세 조회
    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<MenuDetailResponseDto>> getMenu(@PathVariable Long menuId){

        return ResponseEntity.ok(ApiResponse.of(menuService.getMenu(menuId)));
    }

    // 메뉴 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponseDto<MenuSearchResponseDto>>> searchMenus(
            @ModelAttribute MenuSearchRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails != null ? customUserDetails.getUser().getId() :  null;
        Pageable pageable = PageRequest.of(requestDto.page() - 1, requestDto.size());
        return ResponseEntity.ok(ApiResponse.of(menuService.searchMenus(requestDto, pageable, userId)));
    }

    // 인기 메뉴 조회
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularMenuResponseDto>>> getPopularMenus() {

        List<PopularMenuResponseDto> data = menuPopularService.getPopularMenus(3);
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
