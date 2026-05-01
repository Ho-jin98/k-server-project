package com.example.kserverproject.common.response;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record ApiResponse<T> (

        boolean success,
        T data
) {
    public static <T> ApiResponse<T> of(T data) {
        return  ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> empty() {
        return ApiResponse.<T>builder()
                .success(true)
                .data(null)
                .build();
    }
}
