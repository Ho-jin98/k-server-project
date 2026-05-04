package com.example.kserverproject.domain.menu.dto.response;

import com.example.kserverproject.domain.menu.entity.Menu;

public record MenuDetailResponseDto (

        Long menuId,
        String menuName,
        Long price,
        String imageUrl
) {
    public static MenuDetailResponseDto from(Menu menu) {
        return new MenuDetailResponseDto(
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getImageUrl()
        );
    }
}
