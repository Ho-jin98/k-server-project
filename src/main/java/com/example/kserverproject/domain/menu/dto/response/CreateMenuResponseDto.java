package com.example.kserverproject.domain.menu.dto.response;

import com.example.kserverproject.domain.menu.entity.Menu;

public record CreateMenuResponseDto (

        Long menuId,
        String menuName,
        Long price,
        String imageUrl
) {
    public static CreateMenuResponseDto from(Menu menu) {
        return new CreateMenuResponseDto(
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getImageUrl()
        );
    }
}
