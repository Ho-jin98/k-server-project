package com.example.kserverproject.domain.pointHistory.dto.response;

import com.example.kserverproject.domain.pointHistory.entity.PointHistory;
import com.example.kserverproject.domain.pointHistory.enums.PointType;

import java.time.LocalDateTime;

public record PointHistoryTransactionalResponseDto (

        Long historyId,
        Long amount,
        PointType pointType,
        Long balanceAfter,
        LocalDateTime createdAt
) {
    public static PointHistoryTransactionalResponseDto from(PointHistory pointHistory) {
        return new PointHistoryTransactionalResponseDto(
                pointHistory.getId(),
                pointHistory.getAmount(),
                pointHistory.getPointType(),
                pointHistory.getBalanceAfter(),
                pointHistory.getCreatedAt()
        );
    }
}
