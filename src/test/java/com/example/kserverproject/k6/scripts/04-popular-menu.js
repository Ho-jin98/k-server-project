// 인기 메뉴 (Redis ZSet + result키)
// ====================================================================
// 04. 인기 메뉴 조회 테스트 (Redis Sorted Set + result 키 캐싱 검증)
// ====================================================================
// 목적:
//   - 100 VU 동시 인기메뉴 조회에도 응답이 일관적이고 빠른지 검증
//   - result 키 캐싱이 동작하면 부하가 몰려도 ZUNIONSTORE는 최대 1번
//   - 응답이 모든 VU에서 동일해야 함 (data consistency)
//
// 시나리오:
//   1) 사전: 주문 몇 건 생성 (인기메뉴 데이터 시드)
//   2) 100 VU가 동시에 /api/menus/popular 호출
//   3) 모든 응답이 동일한 TOP 3을 반환해야 함
//   4) p95 응답시간 50ms 이하 (result 키 캐시 적중 시)
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 scripts/04-popular-menu.js
// ====================================================================

import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { COMMON_THRESHOLDS } from '../lib/config.js';
import { signupAndLogin, chargePoint } from '../lib/auth.js';
import { getAllMenus, createOrder, getPopularMenus } from '../lib/helpers.js';

// ============== 커스텀 메트릭 ==============
const popularDuration = new Trend('popular_menu_duration');
const popularConsistency = new Counter('popular_consistency_match');
const popularInconsistency = new Counter('popular_consistency_mismatch');

// ============== 테스트 설정 ==============
const VU_COUNT = 100;
const SEED_ORDERS_PER_MENU = 5; // 메뉴별로 5건씩 주문 시드

export const options = {
    scenarios: {
        popular_load: {
            executor: 'constant-vus',
            vus: VU_COUNT,
            duration: '1m',
        },
    },
    thresholds: {
        ...COMMON_THRESHOLDS,
        // result 키 캐싱이 동작하면 매우 빨라야 함
        'popular_menu_duration': ['p(95)<100', 'p(99)<300'],
        'popular_consistency_mismatch': ['count<5'], // 캐시 갱신 타이밍 일부 허용
        'http_req_failed': ['rate<0.01'],
    },
};

// ============== setup: 인기메뉴 데이터 시드 ==============
export function setup() {
    console.log('[setup] 인기메뉴 시드 데이터 생성 시작');

    // 1) 시드용 유저 생성 + 충분한 포인트 충전
    const email = 'user1@test.com';
    const token = signupAndLogin(email, 'password123!', 'popular-seed');
    if (!token) throw new Error('시드 유저 생성 실패');

    // 2) 메뉴 조회
    const menus = getAllMenus();
    if (menus.length < 3) {
        throw new Error('메뉴가 3개 미만입니다. 먼저 ADMIN으로 메뉴를 3개 이상 등록해주세요.');
    }

    // 3) 충분한 포인트 충전 (모든 메뉴 시드 주문 가능하도록)
    const totalNeeded = menus.reduce((sum, m) => sum + (m.price * SEED_ORDERS_PER_MENU), 0);
    chargePoint(token, totalNeeded);

    // 4) 메뉴별로 SEED_ORDERS_PER_MENU건씩 주문 (인기메뉴 카운터 누적)
    console.log(`[setup] 메뉴 ${menus.length}개에 대해 각 ${SEED_ORDERS_PER_MENU}건씩 주문 시드`);
    for (const menu of menus) {
        for (let i = 0; i < SEED_ORDERS_PER_MENU; i++) {
            const res = createOrder(token, [{ menuId: menu.menuId, quantity: 1 }]);
            if (res.status !== 201) {
                console.warn(`[seed] ${menu.menuName} 주문 실패 (${i+1}/${SEED_ORDERS_PER_MENU}): ${res.status}`);
            }
        }
    }

    // 5) Kafka Consumer가 ZINCRBY 처리할 시간 대기
    console.log('[setup] Kafka Consumer 처리 대기 (5초)');
    sleep(5);

    // 6) 첫 인기메뉴 조회로 result 키 워밍업
    const firstRes = getPopularMenus();
    if (firstRes.status !== 200) {
        throw new Error(`인기메뉴 조회 실패: ${firstRes.status}`);
    }

    const baseline = firstRes.json().data;
    console.log('[setup] 베이스라인 인기메뉴:');
    if (Array.isArray(baseline)) {
        baseline.forEach(m => console.log(`  ${m.rank}위: ${m.menuName} (${m.orderCount}회)`));
    }

    // 베이스라인을 직렬화해서 비교용으로 저장
    return {
        baselineJson: JSON.stringify(baseline?.map(m => m.menuId) || []),
    };
}

// ============== default: 동시 인기메뉴 조회 ==============
export default function (data) {
    const start = Date.now();
    const res = getPopularMenus();
    const duration = Date.now() - start;
    popularDuration.add(duration);

    check(res, {
        '인기메뉴 조회 성공 (200)': (r) => r.status === 200,
        'data 배열 존재': (r) => Array.isArray(r.json().data),
    });

    // 일관성 체크: 베이스라인과 menuId 순서가 같은지
    if (res.status === 200) {
        const current = res.json().data;
        const currentIds = JSON.stringify(current?.map(m => m.menuId) || []);

        if (currentIds === data.baselineJson) {
            popularConsistency.add(1);
        } else {
            popularInconsistency.add(1);
            // 디버그용 (너무 많이 찍히면 주석 처리)
            // console.warn(`[VU ${__VU}] 베이스라인과 다름: ${currentIds}`);
        }
    }
}

// ============== teardown ==============
export function teardown(data) {
    console.log('===========================================');
    console.log('result 키 캐싱 검증:');
    console.log('  → popular_consistency_match가 mismatch보다 압도적으로 많아야 함');
    console.log('  → p95 응답시간이 100ms 이하면 캐시 적중 정상');
    console.log('===========================================');
}