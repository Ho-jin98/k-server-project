package com.example.kserverproject.domain.auth.dto.response;

import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.enums.UserRole;

public record SignupResponseDto(

        Long userId,

        String email,

        String nickname,

        UserRole role

) {
    public static SignupResponseDto from(User user) {
        return new SignupResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole()
        );
    }
}
