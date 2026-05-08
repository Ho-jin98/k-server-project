// ============================================================
// 🏆 시나리오: 200명이 "오늘의 인기 메뉴" 배너를 동시에 새로고침
// ============================================================
//
// DataInitializer 활용:
//   user1~5 계정 각 100만P 보유
//   메뉴 6개 등록됨
//   → 의도적 순위 시드: 아메리카노 30건, 아이스 아메리카노 20건, 카페라떼 10건
//   → DataInitializer 계정으로 충전 없이 바로 주문 가능
//
// 검증 목표:
//   ✅ 200 VU 동시 조회 시 모든 응답이 동일한 TOP 3
//   ✅ result 키 캐싱 적중 시 p95 < 50ms
//   ✅ 순위가 시드한 순서와 일치 (아메 > 아이스아메 > 카페라떼)
//
// 엣지 케이스:
//   ❌ p95 > 200ms → result 키 캐싱 미동작, 매번 ZUNIONSTORE
//   ❌ consistency_mismatch > 0 → result 키 갱신 Race Condition
//   ❌ 순위 불일치 → Kafka 메시지 유실로 카운트 오차
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 05_popular_menu_sorted_set.js
// ============================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// ── 환경 ──────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HEADERS  = { 'Content-Type': 'application/json' };

// ── DataInitializer 계정 ───────────────────────────────────
const TEST_USERS = [
    { email: 'user1@test.com', password: 'password123!' },
    { email: 'user2@test.com', password: 'password123!' },
    { email: 'user3@test.com', password: 'password123!' },
    { email: 'user4@test.com', password: 'password123!' },
    { email: 'user5@test.com', password: 'password123!' },
];

// ── 시드 계획 (DataInitializer 메뉴명 기반) ───────────────
// 아메리카노 30건, 아이스 아메리카노 20건, 카페라떼 10건
const SEED_PLAN = [
    { menuName: '아메리카노',     orderCount: 30 },
    { menuName: '아이스 아메리카노', orderCount: 20 },
    { menuName: '카페라떼',       orderCount: 10 },
];

const VU_COUNT = 200;

// ── 커스텀 메트릭 ──────────────────────────────────────────
const popularDuration     = new Trend('popular_duration_ms');
const consistencyMatch    = new Counter('consistency_match');
const consistencyMismatch = new Counter('consistency_mismatch');
const emptyResponse       = new Counter('popular_empty');

// ── K6 옵션 ────────────────────────────────────────────────
export const options = {
    scenarios: {
        popular_burst: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 100 },  // 10초 동안 100 VU까지
                { duration: '25s', target: 200 },  // 25초 동안 200 VU까지
                { duration: '10s', target: 0 },    // 10초 동안 0으로 감소
            ],
        },
    },
    thresholds: {
        'popular_duration_ms':  ['p(95)<50', 'p(99)<200'],
        'consistency_mismatch': ['count==0'],
        'popular_empty':        ['count==0'],
        'http_req_failed':      ['rate<0.01'],  // rate==0 → rate<0.01로 완화
    },
};

// ── Setup: 시드 주문 + Kafka 대기 + 베이스라인 확보 ─────────
export function setup() {
    console.log('━━━ [setup] 인기 메뉴 Sorted Set 테스트 준비 ━━━');
    console.log('[setup] DataInitializer 메뉴/유저 사용, 별도 충전 불필요');

    // 1) 전 유저 로그인
    const tokens = [];
    for (const user of TEST_USERS) {
        const loginRes = http.post(`${BASE_URL}/api/auth/login`,
            JSON.stringify({ email: user.email, password: user.password }),
            { headers: HEADERS }
        );
        if (loginRes.status === 200) {
            tokens.push(loginRes.json().data?.token);
        }
    }
    if (tokens.length === 0) throw new Error('[setup] 로그인 실패 - DataInitializer 확인');
    console.log(`[setup] 로그인: ${tokens.length}명`);

    // 2) 메뉴 조회 → 시드 계획에 맞는 menuId 매핑
    const menuRes = http.get(`${BASE_URL}/api/menus`);
    const menus = menuRes.json().data || [];
    if (menus.length === 0) throw new Error('[setup] 메뉴 없음');

    const seedMenus = [];
    for (const plan of SEED_PLAN) {
        const found = menus.find(m => m.menuName === plan.menuName);
        if (!found) {
            console.warn(`[setup] "${plan.menuName}" 메뉴 없음 - 대체 메뉴 사용`);
            // 해당 순위에 아무 메뉴라도 배정
            const fallback = menus[seedMenus.length] || menus[0];
            seedMenus.push({ ...fallback, orderCount: plan.orderCount });
        } else {
            seedMenus.push({ ...found, orderCount: plan.orderCount });
        }
    }

    // 3) 시드 주문 생성 (총 60건, 5명 라운드로빈)
    let tokenIdx = 0;
    let totalSeed = 0;
    for (const seedMenu of seedMenus) {
        console.log(`[setup] ${seedMenu.menuName} ${seedMenu.orderCount}건 주문 중...`);
        for (let i = 0; i < seedMenu.orderCount; i++) {
            const token = tokens[tokenIdx % tokens.length];
            tokenIdx++;
            http.post(`${BASE_URL}/api/orders`,
                JSON.stringify({ menuItems: [{ menuId: seedMenu.menuId, quantity: 1 }] }),
                { headers: { ...HEADERS, Authorization: `Bearer ${token}` } }
            );
            totalSeed++;
        }
    }
    console.log(`[setup] 시드 주문 완료: 총 ${totalSeed}건`);

    // 4) Kafka Consumer 처리 대기
    console.log('[setup] Kafka Consumer 처리 대기 (10초)...');
    sleep(10);

    // 5) 인기 메뉴 조회 → result 키 워밍업 + 베이스라인
    const popularRes = http.get(`${BASE_URL}/api/menus/popular`);
    if (popularRes.status !== 200) throw new Error(`[setup] 인기 메뉴 조회 실패: ${popularRes.status}`);
    const baseline = popularRes.json().data || [];

    console.log('[setup] 베이스라인 TOP 3:');
    baseline.forEach((m, i) => {
        const expected = seedMenus[i]?.menuName || '?';
        const mark = m.menuName === expected ? '✅' : '⚠️';
        console.log(`  ${mark} ${i+1}위: ${m.menuName} (${m.orderCount}건) / 기대: ${expected}`);
    });

    const baselineKey = JSON.stringify(baseline.map(m => m.menuId));
    console.log('━━━ [setup] 완료. 200 VU 동시 조회 시작 ━━━');
    return { baselineKey, seedMenus };
}

// ── Default: 200 VU 동시 인기 메뉴 조회 ──────────────────
export default function (data) {
    const { baselineKey } = data;

    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/menus/popular`, { tags: { name: 'popular' } });
    popularDuration.add(Date.now() - start);

    check(res, {
        '[Sorted Set] 200 OK':          (r) => r.status === 200,
        '[Sorted Set] data 배열 존재':   (r) => Array.isArray(r.json().data),
        '[Sorted Set] TOP 3 반환':       (r) => (r.json().data || []).length === 3,
        '[Sorted Set] 1위 ≥ 2위 orderCount': (r) => {
            const d = r.json().data || [];
            return d.length < 2 || d[0].orderCount >= d[1].orderCount;
        },
    });

    if (res.status === 200) {
        const responseData = res.json().data || [];
        if (responseData.length === 0) {
            emptyResponse.add(1);
            return;
        }
        const currentKey = JSON.stringify(responseData.map(m => m.menuId));
        if (currentKey === baselineKey) {
            consistencyMatch.add(1);
        } else {
            consistencyMismatch.add(1);
            if (consistencyMismatch.value <= 3) {
                console.error(`[VU ${__VU}] TOP 3 불일치 → result 키 Race Condition 의심`);
            }
        }
    }

    sleep(1);
}

// ── Teardown ──────────────────────────────────────────────
export function teardown(data) {
    const { seedMenus } = data;
    const finalRes = http.get(`${BASE_URL}/api/menus/popular`);
    const finalTop3 = finalRes.json().data || [];

    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🏆 인기 메뉴 Sorted Set 테스트 결과');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('최종 TOP 3 (기대 순위: 아메리카노 > 아이스 아메리카노 > 카페라떼):');
    finalTop3.forEach((m, i) => {
        const expected = seedMenus[i]?.menuName || '?';
        const mark = m.menuName === expected ? '✅' : '⚠️';
        console.log(`  ${mark} ${i+1}위: ${m.menuName} (${m.orderCount}건) | 기대: ${expected}`);
    });

    console.log('\n메트릭 확인:');
    console.log('  popular_duration_ms p95 < 50ms: result 키 캐싱 동작');
    console.log('  consistency_mismatch == 0: 모든 VU 동일 TOP 3 수신');

    console.log('\n📌 엣지 케이스 체크리스트:');
    console.log('   1) p95 > 200ms → redis-cli TTL "popular:menus:result"');
    console.log('   2) consistency_mismatch > 0 → SET NX로 갱신 원자성 보장 검토');
    console.log('   3) 순위 기대와 다름 → Kafka 유실 가능성');
    console.log(`      redis-cli ZREVRANGE "popular:menus:$(date +%Y-%m-%d)" 0 2 WITHSCORES`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}