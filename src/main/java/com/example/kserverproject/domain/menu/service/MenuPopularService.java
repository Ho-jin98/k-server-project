package com.example.kserverproject.domain.menu.service;

import com.example.kserverproject.domain.menu.dto.response.PopularMenuResponseDto;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuPopularService {

    private final MenuRedisService menuRedisService;
    private final MenuRepository menuRepository;

    private static final int CACHE_TTL_HOURS = 1;

    // 인기 메뉴 TOP N 상세 정보 조회
    public List<PopularMenuResponseDto> getPopularMenus(int limit) {

        // Redis에서 순위 데이터 조회
        Set<TypedTuple<String>> ranking = menuRedisService.getRankRange(limit);

        // 데이터가 없으면 합산 연산 실행 후 다시 조회
        if (ranking == null || ranking.isEmpty()) {
            menuRedisService.totalWeeklyPopularDate(CACHE_TTL_HOURS);
            ranking = menuRedisService.getRankRange(limit);
        }

        if (ranking == null || ranking.isEmpty()) {
            return Collections.emptyList();
        }

        // ID 리스트 추출
        List<Long> menuIds = ranking.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .toList();

        // DB에서 메뉴 정보 한번에 조회 (IN 쿼리)
        Map<Long, Menu> menuMap = menuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, menu -> menu));

        // Redis 순서를 유지하며 응답 객체 생성
        List<PopularMenuResponseDto> responses = new ArrayList<>();
        int currentRank = 1;

        for (TypedTuple<String> tuple : ranking) {
            Long id = Long.valueOf(tuple.getValue());
            Menu menu = menuMap.get(id);

            if (menu != null) {
                responses.add(PopularMenuResponseDto.of(currentRank++, menu, tuple.getScore()));
            }
        }
        return responses;
    }

}

