package com.example.kserverproject.domain.menu.repository;

import com.example.kserverproject.domain.menu.dto.request.MenuSearchRequestDto;
import com.example.kserverproject.domain.menu.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MenuQueryRepository {

    Page<Menu> searchMenus(MenuSearchRequestDto requestDto, Pageable pageable);
}
