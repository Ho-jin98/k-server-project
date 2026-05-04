package com.example.kserverproject.domain.menu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMenuRequestDto(

        @NotBlank(message = "메뉴 이름은 필수입니다.")
        @Size(min = 2, max = 20, message = "메뉴 이름은 2글자 ~ 20글자 입니다.")
        String menuName,

        @NotNull(message = "메뉴 가격은 필수입니다.")
        @Positive(message = "메뉴가격은 0보다 커야합니다.")
        Long price,

        @NotBlank(message = "메뉴 이미지는 필수입니다.")
        String imageUrl
) {}
