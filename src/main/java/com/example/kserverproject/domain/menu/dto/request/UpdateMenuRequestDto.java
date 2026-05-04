package com.example.kserverproject.domain.menu.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateMenuRequestDto (

        @Size(min = 2, max = 20, message = "메뉴 이름은 2글자 ~ 20글자 입니다.")
        String menuName,

        @Positive(message = "메뉴가격은 0보다 커야합니다.")
        Long price,

        String imageUrl
) {}
