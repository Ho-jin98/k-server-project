package com.example.kserverproject.domain.menu.dto.request;

public record MenuSearchRequestDto (
        String keyword,
        Long minPrice,
        Long maxPrice,
        int page,
        int size
) {}
