// 메뉴 캐시 (Redis @Cacheable)
// ====================================================================
// 03. 메뉴 캐시 부하 테스트 (Redis @Cacheable 검증)
// ====================================================================
// 목적:
//   - 메뉴 조회 API가 Redis 캐시 hit률에 따라 응답 시간이
//     안정적으로 유지되는지 검증
//   - 점진적 부하 증가 (50 → 200 VU)에도 p95 응답시간 유지
//
// 시나리오:
//   1) Cold start: 첫 요청 (캐시 miss → DB 조회)
//   2) Warm: 같은 요청 반복 (캐시 hit)
//   3) Ramp-up: 50 → 100 → 200 VU 점진 증가
//
// 부하 강도:
//   - 가상유저 5,000명은 부담 → 200까지만 ramp
//   - 메뉴 조회는 캐시 hit이면 매우 빠르므로 200 VU도 충분히 감내 가능
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 scripts/03-menu-cache.js
// ====================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL, COMMON_THRESHOLDS, HEADERS_JSON } from '../lib/config.js';
import { getMenus, searchMenu, getMenuDetail } from '../lib/helpers.js';

// ============== 커스텀 메트릭 ==============
const menuListDuration = new Trend('menu_list_duration');
const menuSearchDuration = new Trend('menu_search_duration');
const menuDetailDuration = new Trend('menu_detail_duration');
const cacheHitFast = new Counter('cache_hit_likely');     // 50ms 이하 응답 (캐시 hit 추정)
const cacheMissSlow = new Counter('cache_miss_likely');   // 200ms 이상 (DB 조회 추정)

// ============== 테스트 설정 ==============
export const options = {
    scenarios: {
        cache_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 50 },   // 워밍업: 50 VU까지
                { duration: '30s', target: 100 },  // 중간 부하
                { duration: '30s', target: 200 },  // 최대 부하
                { duration: '20s', target: 0 },    // 정리
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        ...COMMON_THRESHOLDS,
        'menu_list_duration': ['p(95)<200', 'p(99)<500'],     // 캐시 hit이면 매우 빨라야 함
        'menu_detail_duration': ['p(95)<200'],
        'http_req_failed': ['rate<0.01'],
    },
};

// ============== setup: 캐시 워밍업 ==============
export function setup() {
    console.log('[setup] 캐시 워밍업 시작');

    // 사전 1회 호출로 캐시 워밍 (cold start 효과 측정 후 본 테스트는 warm 상태)
    const coldStart = Date.now();
    const res = getMenus();
    const coldDuration = Date.now() - coldStart;

    if (res.status !== 200) {
        throw new Error(`메뉴 조회 실패 - 메뉴를 먼저 등록해주세요: ${res.status}`);
    }

    const menus = res.json().data || [];
    console.log(`[setup] cold start 응답시간: ${coldDuration}ms, 메뉴 개수: ${menus.length}`);

    // 검색 키워드 후보 (캐시 키 분리 확인용)
    const keywords = ['아메', '라떼', '카푸', '바닐라', '에스프'];

    // 메뉴 ID 후보 (단건 조회용)
    const menuIds = menus.slice(0, 5).map(m => m.menuId);

    return { keywords, menuIds, coldDuration };
}

// ============== default: 메뉴 조회 부하 ==============
export default function (data) {
    // 3가지 패턴을 랜덤하게 섞어서 호출 (실제 사용 패턴 모사)
    const pattern = Math.random();

    if (pattern < 0.5) {
        // 50%: 메뉴 목록 조회
        const start = Date.now();
        const res = getMenus();
        const duration = Date.now() - start;
        menuListDuration.add(duration);

        check(res, { '메뉴 목록 조회 성공': (r) => r.status === 200 });
        classifyCacheBehavior(duration);

    } else if (pattern < 0.8) {
        // 30%: 메뉴 검색 (다양한 키워드)
        const keyword = data.keywords[Math.floor(Math.random() * data.keywords.length)];
        const start = Date.now();
        const res = searchMenu(keyword);
        const duration = Date.now() - start;
        menuSearchDuration.add(duration);

        check(res, { '메뉴 검색 성공': (r) => r.status === 200 });

    } else {
        // 20%: 메뉴 상세 조회
        if (data.menuIds.length > 0) {
            const menuId = data.menuIds[Math.floor(Math.random() * data.menuIds.length)];
            const start = Date.now();
            const res = getMenuDetail(menuId);
            const duration = Date.now() - start;
            menuDetailDuration.add(duration);

            check(res, { '메뉴 상세 조회 성공': (r) => r.status === 200 });
            classifyCacheBehavior(duration);
        }
    }

    // 너무 공격적이지 않도록 약간의 think time
    sleep(Math.random() * 0.5);
}

// 캐시 hit/miss 휴리스틱 분류 (정확한 판단은 아니지만 추세 확인용)
function classifyCacheBehavior(duration) {
    if (duration < 50) cacheHitFast.add(1);
    else if (duration > 200) cacheMissSlow.add(1);
}

// ============== teardown ==============
export function teardown(data) {
    console.log('===========================================');
    console.log(`Cold start 응답시간: ${data.coldDuration}ms`);
    console.log('TIP: K6 결과의 menu_list_duration p95와 비교');
    console.log('  → cold start보다 p95가 훨씬 작아야 캐시 효과 확인됨');
    console.log('  → cache_hit_likely / cache_miss_likely 비율도 함께 확인');
    console.log('===========================================');
}