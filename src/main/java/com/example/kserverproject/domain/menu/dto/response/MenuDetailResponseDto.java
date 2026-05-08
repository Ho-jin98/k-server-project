package com.example.kserverproject.domain.menu.dto.response;

import com.example.kserverproject.domain.menu.entity.Menu;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuDetailResponseDto {

    private Long menuId;
    private String menuName;
    private Long price;
    private String imageUrl;

    public static MenuDetailResponseDto from(Menu menu) {
        return new MenuDetailResponseDto(
                menu.getId(),
                menu.getMenuName(),
                menu.getPrice(),
                menu.getImageUrl()
        );
    }
}
