package com.example.kserverproject.domain.pointHistory.dto.response;

import com.example.kserverproject.domain.user.entity.User;

public record PointChargeResponseDto (

        Long userId,
        Long chargeAmount,
        Long pointBalance
) {
    public static PointChargeResponseDto of(User user, Long amount) {
        return new PointChargeResponseDto(
                user.getId(),
                amount,
                user.getPointBalance()
        );
    }
}
