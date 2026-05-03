package com.example.kserverproject.common.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponseDto<T> (

        List<T> content,
        int page,
        int size,
        long totalElements
) {
    public static <T> PageResponseDto<T> of (Page<T> page) {
        return new PageResponseDto<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements()
        );
    }
}
