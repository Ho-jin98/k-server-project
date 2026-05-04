package com.example.kserverproject.domain.menu.dto.response;

import com.example.kserverproject.domain.menu.entity.Menu;

public record UpdateMenuResponseDto (

        Long menuId,
        String menuName,
        Long price,
        String imageUrl
) {
    public static UpdateMenuResponseDto from(Menu menu) {
        return new UpdateMenuResponseDto(
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getImageUrl()
        );
    }
}
