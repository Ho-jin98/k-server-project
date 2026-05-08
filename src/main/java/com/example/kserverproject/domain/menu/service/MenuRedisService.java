package com.example.kserverproject.domain.menu.service;

import com.example.kserverproject.domain.menu.dto.response.MenuDetailResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuListResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MenuRedisService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // 날짜별 점수 저장하는 키
    private static final String DAILY_KEY_PREFIX = "popular:menus:";
    // 7일 합산 결과 캐싱 키
    private static final String RESULT_KEY = "popular:menus:result";
    // 중복 방지용 키
    private static final String DEDUP_PREFIX = "dedup:menu:";

    // 5분간 중복 카운팅 방지
    private static final long DEDUP_TTL_MINUTES = 5;
    // 집계 기간 7일 설정
    private static final int TOTAL_DAYS = 7;

    private static final String MENUS_ALL_KEY = "menus:all";
    private static final String MENU_DETAIL_KEY_PREFIX = "menu:";
    private static final long TTL_MINUTES = 30;

    // 메뉴 목록 캐시
    public List<MenuListResponseDto> getMenusCache() {
        String cached = stringRedisTemplate.opsForValue().get(MENUS_ALL_KEY);
        if (cached == null) {return null;}
        try {
            return objectMapper.readValue(cached, objectMapper.getTypeFactory().constructCollectionType(List.class, MenuListResponseDto.class));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void setMenusCache(List<MenuListResponseDto> menus) {
        try {
            stringRedisTemplate.opsForValue().set(
                    MENUS_ALL_KEY, objectMapper.writeValueAsString(menus), Duration.ofMinutes(TTL_MINUTES));
        } catch (JsonProcessingException ignored) {}
    }

    // 메뉴 단건 캐시
    public MenuDetailResponseDto getMenuCache(Long menuId) {
        String cached = stringRedisTemplate.opsForValue().get(MENU_DETAIL_KEY_PREFIX + menuId);
        if (cached == null) {return null;}
        try {
            return objectMapper.readValue(cached, MenuDetailResponseDto.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void setMenuCache(Long menuId, MenuDetailResponseDto responseDto) {
        try {
            stringRedisTemplate.opsForValue().set(
                    MENU_DETAIL_KEY_PREFIX + menuId, objectMapper.writeValueAsString(responseDto), Duration.ofMinutes(TTL_MINUTES));
        } catch (JsonProcessingException ignored) {}
    }

    // 캐시 Evict

    public void evictMenusAll() {
        stringRedisTemplate.delete(MENUS_ALL_KEY);
    }

    public void evictMenu(Long menuId) {
        stringRedisTemplate.delete(MENU_DETAIL_KEY_PREFIX + menuId);
    }


    // 검색 중복 방지용
    // 인기 메뉴 스코어 1씩 증가 (중복 방지 포함)
    // menuId -> 메뉴 식별자, userId -> 사용자 식별자
    public void incrementMenuScoreBySearch(Long menuId, String userId) {

        String dedupKey = DEDUP_PREFIX + menuId + ":" + userId;

        // 중복 방지 setIfAbsent를 이용해 5분간 해당 사용자의 동일 메뉴 점수 조작 방지
        Boolean isFirstSearch = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofMinutes(DEDUP_TTL_MINUTES));

        if (Boolean.TRUE.equals(isFirstSearch)) {
            // 시간 제외, 날짜까지만 키로 사용 (ex: popular:menus:2026-05-04)
            String dateKey = DAILY_KEY_PREFIX + LocalDate.now().toString();

            // 해당 날짜 키의 메뉴 점수 1 증가
            stringRedisTemplate.opsForZSet().incrementScore(dateKey, menuId.toString(), 1);

            // 날짜별 키에 8일 (7일 + 마진 1일) TTL 설정
            stringRedisTemplate.expire(dateKey, Duration.ofDays(TOTAL_DAYS + 1));
        }
    }

    // 주문용 - 중복 방지 없이 바로 카운트
    public void incrementMenuScoreByOrderCreate(Long menuId, String userId) {
        String dataKey = DAILY_KEY_PREFIX + LocalDate.now().toString();
        stringRedisTemplate.opsForZSet().incrementScore(dataKey, menuId.toString(), 1);
        stringRedisTemplate.expire(dataKey, Duration.ofDays(TOTAL_DAYS + 1));
    }

    // 결과 키 RESULT_KEY에서 데이터 조회
    public Set<TypedTuple<String>> getRankRange(int limit) {
        return stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RESULT_KEY, 0 , limit - 1);
    }

    // 최근 7일치 날짜별 키를 합산하여 결과 RESULT_KEY를 생성
    public void totalWeeklyPopularDate(int cacheHours) {
        // 오늘부터 과거 7일치 키 리스트 생성
        List<String> keys = IntStream.range(0, TOTAL_DAYS)
                .mapToObj(i -> DAILY_KEY_PREFIX + LocalDate.now().minusDays(i).toString())
                .filter(key -> Boolean.TRUE.equals(stringRedisTemplate.hasKey(key)))
                .toList();

        if (keys.isEmpty()) {
            return;
        }

        if (keys.size() == 1) {
            Set<TypedTuple<String>> single = stringRedisTemplate.opsForZSet()
                    .rangeWithScores(keys.get(0), 0, -1);

            if (single != null && !single.isEmpty()) {
                stringRedisTemplate.opsForZSet().add(RESULT_KEY, single);
                stringRedisTemplate.expire(RESULT_KEY, Duration.ofHours(cacheHours));
            }
            return;
        }
        // 여러 날짜의 키를 합쳐서 RESULT_KEY에 저장
        stringRedisTemplate.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), RESULT_KEY);

        // 결과는 설정된 시간(1시간) 동안 캐싱
        stringRedisTemplate.expire(RESULT_KEY, Duration.ofHours(cacheHours));
    }
}
