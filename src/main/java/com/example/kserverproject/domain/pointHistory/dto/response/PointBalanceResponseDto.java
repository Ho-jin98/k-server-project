package com.example.kserverproject.domain.pointHistory.dto.response;

import com.example.kserverproject.domain.user.entity.User;

public record PointBalanceResponseDto (

        Long userId,

        Long pointBalance
) {
    public static PointBalanceResponseDto from(User user) {
        return new PointBalanceResponseDto(
                user.getId(),
                user.getPointBalance()
        );
    }
}
