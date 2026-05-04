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

    @Cacheable(value = "menus", cacheManager = "cacheManager")
    public List<MenuListResponseDto> getMenus() {

        return menuRepository.findAll().stream()
                .map(MenuListResponseDto::from)
                .toList();
    }

    // 메뉴 상세 조회
    @Cacheable(value = "menus", key = "#menuId", cacheManager = "cacheManager")
    public MenuDetailResponseDto getMenu(Long menuId) {

        Menu findMenu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        return MenuDetailResponseDto.from(findMenu);
    }

    // 메뉴 검색
    public PageResponseDto<MenuSearchResponseDto> searchMenus(MenuSearchRequestDto requestDto, Pageable pageable) {

        Page<Menu> page = menuRepository.searchMenus(requestDto, pageable);

        Page<MenuSearchResponseDto> map = page.map(MenuSearchResponseDto::from);

        return PageResponseDto.of(map);
    }
}
