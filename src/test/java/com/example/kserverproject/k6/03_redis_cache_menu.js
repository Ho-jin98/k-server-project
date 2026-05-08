// ============================================================
// 🍽️ 시나리오: 출근 러시, 200명이 메뉴판을 동시에 펼친다
// ============================================================
//
// 배경:
//   평일 오전 8시 50분, 지하철 안에서 회사원들이 앱을 열어 메뉴를 미리 본다.
//   메뉴는 하루 1번 바뀔까 말까 한 정적 데이터지만, 조회는 수천 번 발생.
//
//   @Cacheable 없으면: 매 요청마다 DB 풀스캔 → DB 과부하
//   @Cacheable 있으면: 첫 요청만 DB 조회(Miss), 이후는 Redis(Hit)
//
// DataInitializer 활용:
//   메뉴 6개 이미 등록됨 (아메리카노 3,000P ~ 카라멜 마키아토 5,000P)
//   인증 불필요한 GET API라 로그인 없이 바로 테스트 가능
//
// 검증 목표:
//   ✅ Cache Hit p95 < 100ms
//   ✅ 200 VU 동시 조회 중 모든 응답 데이터 일치 (캐시 오염 없음)
//   ✅ 메뉴 6개 정상 반환
//   ✅ 에러율 < 0.1%
//
// 엣지 케이스:
//   ❌ p95 > 300ms → @Cacheable 미적용 or Redis 연결 문제
//   ❌ consistency_fail > 0 → 캐시 키 충돌 or @CacheEvict 과다 호출
//   ❌ 메뉴 수 != 6 → DataInitializer 미실행 or 캐시에 이전 데이터 남음
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 03_redis_cache_menu.js
// ============================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// ── 환경 ──────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HEADERS  = { 'Content-Type': 'application/json' };

// DataInitializer 기준 메뉴 수
const EXPECTED_MENU_COUNT = 6;

// ── 커스텀 메트릭 ──────────────────────────────────────────
const menuListDuration   = new Trend('menu_list_duration_ms');
const menuDetailDuration = new Trend('menu_detail_duration_ms');
const menuSearchDuration = new Trend('menu_search_duration_ms');
const cacheHit           = new Counter('cache_hit_estimated');   // 50ms 이하
const cacheMiss          = new Counter('cache_miss_estimated');  // 200ms 초과
const consistencyPass    = new Counter('consistency_pass');
const consistencyFail    = new Counter('consistency_fail');

// ── K6 옵션 ────────────────────────────────────────────────
export const options = {
    scenarios: {
        menu_list_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50  },
                { duration: '30s', target: 200 },
                { duration: '10s', target: 0   },
            ],
        },
        // 동일 키워드("아메") 반복 검색 → 검색 캐시 효과 확인
        menu_search_repeat: {
            executor:  'constant-vus',
            vus:       50,
            duration:  '30s',
            startTime: '20s',
        },
    },
    thresholds: {
        'menu_list_duration_ms':   ['p(95)<100', 'p(99)<300'],
        'menu_detail_duration_ms': ['p(95)<100'],
        'menu_search_duration_ms': ['p(95)<200'],
        'http_req_failed':         ['rate<0.001'],
        'consistency_fail':        ['count==0'],
    },
};

// ── Setup: 메뉴 확인 + Cold/Warm 베이스라인 측정 ──────────
export function setup() {
    console.log('━━━ [setup] Redis 캐시 메뉴 조회 테스트 준비 ━━━');
    console.log('[setup] DataInitializer 메뉴 6개 사용 (별도 등록 불필요)');

    // Cold start 측정
    const coldStart = Date.now();
    const coldRes = http.get(`${BASE_URL}/api/menus`);
    const coldDuration = Date.now() - coldStart;

    if (coldRes.status !== 200) throw new Error(`메뉴 조회 실패: ${coldRes.status}`);
    const menus = coldRes.json().data || [];
    console.log(`[setup] 메뉴 수: ${menus.length}개 (기대: ${EXPECTED_MENU_COUNT}개)`);
    if (menus.length !== EXPECTED_MENU_COUNT) {
        console.warn(`[setup] ⚠️ 메뉴 수 불일치 - DataInitializer 실행 여부 또는 캐시 확인`);
    }
    console.log(`[setup] Cold start 응답: ${coldDuration}ms`);

    // Warm start 측정 (캐시 적중)
    const warmStart = Date.now();
    http.get(`${BASE_URL}/api/menus`);
    const warmDuration = Date.now() - warmStart;
    console.log(`[setup] Warm start 응답:  ${warmDuration}ms`);
    if (warmDuration < coldDuration) {
        console.log(`[setup] ✅ 캐시 효과 확인: ${coldDuration}ms → ${warmDuration}ms`);
    } else {
        console.warn(`[setup] ⚠️ Cold/Warm 응답 유사 → 캐시 미적용 가능성`);
    }

    const menuIds = menus.map(m => m.menuId);
    const baselineMenuIds = JSON.stringify(menus.map(m => m.menuId).sort());
    // DataInitializer에 "아메리카노"가 있으므로 첫 두 글자 "아메"로 검색
    const sampleKeyword = '아메';

    console.log('━━━ [setup] 완료 ━━━');
    return { menuIds, sampleKeyword, baselineMenuIds, coldDuration, warmDuration };
}

// ── Default ────────────────────────────────────────────────
export default function (data) {
    const { menuIds, sampleKeyword, baselineMenuIds } = data;

    if (__ENV.K6_SCENARIO_NAME === 'menu_search_repeat') {
        runSearchTest(sampleKeyword);
        return;
    }
    runMenuListTest(baselineMenuIds, menuIds);
}

function runMenuListTest(baselineMenuIds, menuIds) {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/menus`, { tags: { name: 'menu_list' } });
    const duration = Date.now() - start;
    menuListDuration.add(duration);

    if (duration < 50) {
        cacheHit.add(1);
    } else if (duration > 200) {
        cacheMiss.add(1);
        if (duration > 500) {
            console.warn(`[VU ${__VU}] 메뉴 목록 ${duration}ms → 캐시 미적용 의심`);
        }
    }

    check(res, {
        '[캐싱] 200 OK': (r) => r.status === 200,
        '[캐싱] 메뉴 배열 포함': (r) => Array.isArray(r.json().data),
        '[캐싱] 메뉴 6개': (r) => (r.json().data || []).length === EXPECTED_MENU_COUNT,
    });

    // 데이터 일관성 검증
    if (res.status === 200) {
        const current = JSON.stringify((res.json().data || []).map(m => m.menuId).sort());
        if (current === baselineMenuIds) {
            consistencyPass.add(1);
        } else {
            consistencyFail.add(1);
            console.error(`[VU ${__VU}] 메뉴 목록 불일치! 캐시 오염 의심`);
        }
    }

    // 10% 확률로 메뉴 상세도 조회
    if (menuIds.length > 0 && Math.random() < 0.1) {
        const menuId = menuIds[Math.floor(Math.random() * menuIds.length)];
        const detailStart = Date.now();
        const detailRes = http.get(`${BASE_URL}/api/menus/${menuId}`, { tags: { name: 'menu_detail' } });
        menuDetailDuration.add(Date.now() - detailStart);
        check(detailRes, { '[캐싱] 상세 200 OK': (r) => r.status === 200 });
    }

    sleep(0.1);
}

function runSearchTest(keyword) {
    // "아메"로 검색 → 아메리카노, 아이스 아메리카노 2건 반환 기대 (DataInitializer 기준)
    const start = Date.now();
    const res = http.get(
        `${BASE_URL}/api/menus/search?keyword=${encodeURIComponent(keyword)}`,
        { tags: { name: 'menu_search' } }
    );
    menuSearchDuration.add(Date.now() - start);

    check(res, {
        '[검색 캐싱] 200 OK': (r) => r.status === 200,
        '[검색 캐싱] "아메" 결과 2건': (r) => (r.json().data || []).length === 2,
    });
}

// ── Teardown ──────────────────────────────────────────────
export function teardown(data) {
    const { coldDuration, warmDuration } = data;

    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🍽️ Redis 캐시 메뉴 조회 테스트 결과');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`Cold start: ${coldDuration}ms / Warm start: ${warmDuration}ms`);
    console.log('\n메트릭 확인:');
    console.log('  menu_list_duration_ms p95 < 100ms → 캐시 정상 적중');
    console.log('  cache_hit_estimated >> cache_miss_estimated → 캐시 효과 있음');
    console.log('  consistency_fail == 0 → 캐시 오염 없음');
    console.log('\n📌 엣지 케이스 체크리스트:');
    console.log('   1) p95 > 200ms → redis-cli ping / redis-cli KEYS "menus*"');
    console.log('   2) consistency_fail > 0 → @CacheEvict 과다 호출 확인');
    console.log('   3) 메뉴 6개 아닌 경우 → DataInitializer 실행 여부 확인 (local 프로파일)');
    console.log('   4) Thundering Herd 우려 → Cache Aside + 분산락 조합 검토');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}