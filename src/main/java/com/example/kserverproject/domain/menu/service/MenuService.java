package com.example.kserverproject.domain.menu.service;

import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.menu.dto.request.MenuSearchRequestDto;
import com.example.kserverproject.domain.menu.dto.response.MenuDetailResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuListResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuSearchResponseDto;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuRedisService menuRedisService;

    public List<MenuListResponseDto> getMenus() {
        List<MenuListResponseDto> cached = menuRedisService.getMenusCache();
        if (cached != null) return cached;

        List<MenuListResponseDto> menus = menuRepository.findAll().stream()
                .map(MenuListResponseDto::from)
                .toList();

        menuRedisService.setMenusCache(menus);
        return menus;
    }

    // 메뉴 상세 조회
    public MenuDetailResponseDto getMenu(Long menuId) {
        MenuDetailResponseDto cached = menuRedisService.getMenuCache(menuId);
        if (cached != null) return cached;

        Menu findMenu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        MenuDetailResponseDto responseDto = MenuDetailResponseDto.from(findMenu);
        menuRedisService.setMenuCache(menuId, responseDto);

        return responseDto;
    }

    // 메뉴 검색
    public PageResponseDto<MenuSearchResponseDto> searchMenus(MenuSearchRequestDto requestDto, Pageable pageable, Long userId) {

        Page<MenuSearchResponseDto> result = menuRepository.searchMenus(requestDto, pageable)
                .map(MenuSearchResponseDto::from);

        if (userId != null) {
            result.getContent().forEach(menu ->
                    menuRedisService.incrementMenuScoreBySearch(menu.menuId(), userId.toString()));
        }

        return PageResponseDto.of(result);
    }
}
