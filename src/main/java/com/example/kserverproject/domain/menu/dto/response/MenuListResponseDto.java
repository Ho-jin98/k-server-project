package com.example.kserverproject.domain.menu.dto.response;

import com.example.kserverproject.domain.menu.entity.Menu;

public record MenuListResponseDto (

        Long menuId,
        String menuName,
        Long price,
        String imageUrl
) {
    public static MenuListResponseDto from(Menu menu) {
        return new MenuListResponseDto(
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getImageUrl()
        );
    }
}
