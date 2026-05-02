package com.example.kserverproject.domain.user.dto.response;

import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.enums.UserRole;
import lombok.Builder;

@Builder
public record UserInformationResponseDto(

        Long userId,
        String email,
        String nickname,
        UserRole role
) {
    public static UserInformationResponseDto from(User user) {
        return new UserInformationResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole()
        );
    }
}

