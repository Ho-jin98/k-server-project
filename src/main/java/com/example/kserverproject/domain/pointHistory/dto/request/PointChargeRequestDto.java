package com.example.kserverproject.domain.pointHistory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointChargeRequestDto (

        @NotNull(message = "충전 금액은 필수입니다.")
        @Positive(message = "충전 금액은 0보다 커야합니다.")
        Long amount
) {}
