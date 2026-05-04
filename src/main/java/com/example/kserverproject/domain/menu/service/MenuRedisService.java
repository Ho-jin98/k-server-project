package com.example.kserverproject.domain.menu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MenuRedisService {

    private final StringRedisTemplate stringRedisTemplate;

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


    // 인기 메뉴 스코어 1씩 증가 (중복 방지 포함)
    // menuId -> 메뉴 식별자, userId -> 사용자 식별자
    public void incrementMenuScore(Long menuId, String userId) {

        String dedupKey = DEDUP_PREFIX + menuId + ":" + userId;

        // 중복 방지 setIfAbsent를 이용해 5분간 해당 사용자의 동일 메뉴 점수 조작 방지
        Boolean isFirstSearch = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", DEDUP_TTL_MINUTES, TimeUnit.MINUTES);

        if (Boolean.TRUE.equals(isFirstSearch)) {
            // 시간 제외, 날짜까지만 키로 사용 (ex: popular:menus:2026-05-04)
            String dateKey = DAILY_KEY_PREFIX + LocalDate.now().toString();

            // 해당 날짜 키의 메뉴 점수 1 증가
            stringRedisTemplate.opsForZSet().incrementScore(dateKey, menuId.toString(), 1);

            // 날짜별 키에 8일 (7일 + 마진 1일) TTL 설정
            stringRedisTemplate.expire(dateKey, TOTAL_DAYS + 1, TimeUnit.DAYS);
        }
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
                .toList();

        if (!keys.isEmpty()) {
            // 여러 날짜의 키를 합쳐서 RESULT_KEY에 저장
            stringRedisTemplate.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), RESULT_KEY);

            // 결과는 설정된 시간(1시간) 동안 캐싱
            stringRedisTemplate.expire(RESULT_KEY, cacheHours, TimeUnit.HOURS);
        }
    }
}
