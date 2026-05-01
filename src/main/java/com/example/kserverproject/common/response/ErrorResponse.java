package com.example.kserverproject.common.response;

import lombok.Builder;

@Builder
public record ErrorResponse (

        boolean success,
        ErrorDetails error
) {
    @Builder
    public record ErrorDetails(
            String code,
            String message
    ) {}

    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }
}
