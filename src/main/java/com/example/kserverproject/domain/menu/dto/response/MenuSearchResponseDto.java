package com.example.kserverproject.domain.menu.dto.response;

import com.example.kserverproject.domain.menu.entity.Menu;

public record MenuSearchResponseDto(
        Long menuId,
        String menuName,
        Long price,
        String imageUrl
) {
    public static MenuSearchResponseDto from(Menu menu) {
        return new MenuSearchResponseDto(
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getImageUrl()
        );
    }
}
