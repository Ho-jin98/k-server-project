package com.example.kserverproject.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record SignupRequestDto (

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        String password
) {}
