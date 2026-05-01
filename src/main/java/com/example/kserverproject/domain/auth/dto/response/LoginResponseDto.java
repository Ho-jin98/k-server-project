package com.example.kserverproject.domain.auth.dto.response;

public record LoginResponseDto (

        Long id,

        String grantType,

        String token
) {
    public static LoginResponseDto of(Long id, String token) {
        return new LoginResponseDto(id, "Bearer", token);
    }
}
