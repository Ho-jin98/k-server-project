package com.example.kserverproject.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto (

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호 입력은 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자리 이상 입력해주세요.")
        String password
) {}
