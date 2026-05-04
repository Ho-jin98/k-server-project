package com.example.kserverproject.domain.menu.dto.response;

import com.example.kserverproject.domain.menu.entity.Menu;

public record PopularMenuResponseDto(

        int rank,
        Long menuId,
        String menuName,
        Long price,
        String imageUrl,
        int orderCount
) {
    public static PopularMenuResponseDto of(int rank, Menu menu, Double score) {
        return new PopularMenuResponseDto(
                rank,
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getImageUrl(),
                score!= null ? score.intValue() : 0 // 스코어를 정수형 orderCount로 변환
        );
    }
}
