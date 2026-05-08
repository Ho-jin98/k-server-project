// ============================================================
// 📨 시나리오: 점심 피크, 60건 주문 → Kafka 단 1건도 유실 없는가
// ============================================================
//
// DataInitializer 활용:
//   user1~5 계정 각 100만P 보유 → 별도 충전 불필요
//   메뉴 6개 등록됨 → 가장 저렴한 메뉴(아메리카노 3,000P) 사용
//   5명 × 12건 = 60건 → 각자 36,000P 사용, 100만P 충분
//
// 검증 흐름:
//   1) 베이스라인: 특정 메뉴의 현재 인기 메뉴 카운트 기록
//   2) 60건 주문 발행 (user1~5 라운드로빈)
//   3) Kafka Consumer 10초 대기
//   4) 인기 메뉴 카운트 = 베이스라인 + 60 확인
//
// 검증 목표:
//   ✅ 주문 60건 모두 201
//   ✅ 카운트 증가분 = 정확히 60
//   ✅ kafka_publish_duration_ms p95 < 2초
//
// 엣지 케이스:
//   ❌ 카운트 < 60 → DLT 메시지 확인 필요
//   ❌ 카운트 > 60 → Consumer 중복 처리 (멱등성 문제)
//   ❌ result 키 캐싱으로 인한 즉시 미반영 → Redis CLI 직접 확인 필요
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 04_kafka_integrity.js
// ============================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

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

const VU_COUNT          = 20;
const TOTAL_ORDERS      = 60;
const CONSUMER_WAIT_SEC = 10;

// ── 커스텀 메트릭 ──────────────────────────────────────────
const orderPublished  = new Counter('kafka_order_published');
const orderFailed     = new Counter('kafka_order_failed');
const publishDuration = new Trend('kafka_publish_duration_ms');

// ── K6 옵션 ────────────────────────────────────────────────
export const options = {
    scenarios: {
        kafka_integrity: {
            executor:    'shared-iterations',
            vus:         VU_COUNT,
            iterations:  TOTAL_ORDERS,
            maxDuration: '2m',
        },
    },
    thresholds: {
        'kafka_order_published':    [`count==${TOTAL_ORDERS}`],
        'kafka_order_failed':       ['count==0'],
        'kafka_publish_duration_ms': ['p(95)<2000'],
        'http_req_failed':          ['rate<0.01'],
    },
};

// ── Setup: 로그인 토큰 발급 + 베이스라인 카운트 ─────────────
export function setup() {
    console.log('━━━ [setup] Kafka 정합성 테스트 준비 ━━━');
    console.log('[setup] DataInitializer 유저 5명 사용 (각 100만P 보유)');

    // 1) 유저 5명 로그인
    const tokens = [];
    for (const user of TEST_USERS) {
        const loginRes = http.post(`${BASE_URL}/api/auth/login`,
            JSON.stringify({ email: user.email, password: user.password }),
            { headers: HEADERS }
        );
        if (loginRes.status !== 200) {
            console.warn(`[setup] ${user.email} 로그인 실패: ${loginRes.status}`);
            continue;
        }
        tokens.push(loginRes.json().data?.token);
    }
    if (tokens.length === 0) throw new Error('[setup] 로그인 가능한 유저 없음 - DataInitializer 확인');
    console.log(`[setup] 로그인 성공: ${tokens.length}명`);

    // 2) 메뉴 조회 - 아메리카노(3,000P) 타겟
    const menuRes = http.get(`${BASE_URL}/api/menus`);
    const menus = menuRes.json().data || [];
    const targetMenu = menus.find(m => m.menuName === '아메리카노')
        || menus.reduce((min, m) => m.price < min.price ? m : min, menus[0]);
    console.log(`[setup] 타겟 메뉴: ${targetMenu.menuName} (${targetMenu.price}P)`);

    // 3) 베이스라인 카운트 기록
    const popularRes = http.get(`${BASE_URL}/api/menus/popular`);
    let baselineCount = 0;
    if (popularRes.status === 200) {
        const popular = popularRes.json().data || [];
        const found = popular.find(m => m.menuId === targetMenu.menuId);
        baselineCount = found?.orderCount ?? 0;
        console.log(`[setup] 베이스라인 카운트: ${baselineCount}건`);
    }

    console.log(`[setup] 목표: ${TOTAL_ORDERS}건 주문 → 카운트 +${TOTAL_ORDERS}`);
    console.log('━━━ [setup] 완료 ━━━');
    return { tokens, menuId: targetMenu.menuId, menuName: targetMenu.menuName, baselineCount };
}

// ── Default: 60건 주문 ────────────────────────────────────
export default function (data) {
    const { tokens, menuId } = data;

    // 유저 토큰 라운드로빈 (60건 / 5명 = 12건씩)
    const token = tokens[__VU % tokens.length];
    const headers = { ...HEADERS, Authorization: `Bearer ${token}` };

    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/orders`,
        JSON.stringify({ menuItems: [{ menuId, quantity: 1 }] }),
        { headers }
    );
    publishDuration.add(Date.now() - start);

    if (res.status === 201) {
        orderPublished.add(1);
    } else if (res.status === 409) {
        orderFailed.add(1);
        console.error(`[VU ${__VU}] 잔액 부족(409) - 예상 밖 오류 (유저당 100만P 보유)`);
    } else {
        orderFailed.add(1);
        console.error(`[VU ${__VU}] 주문 실패: ${res.status}`);
    }

    check(res, {
        '[Kafka] 주문 201 Created': (r) => r.status === 201,
        '[Kafka] orderId 포함':     (r) => r.json().data?.orderId !== undefined,
    });
}

// ── Teardown: Consumer 처리 대기 후 카운트 검증 ─────────────
export function teardown(data) {
    const { menuId, menuName, baselineCount, tokens } = data;

    console.log(`\n⏳ Kafka Consumer 처리 대기 (${CONSUMER_WAIT_SEC}초)...`);
    sleep(CONSUMER_WAIT_SEC);

    const popularRes = http.get(`${BASE_URL}/api/menus/popular`);

    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📨 Kafka 정합성 테스트 결과');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`타겟 메뉴:  ${menuName}`);
    console.log(`베이스라인: ${baselineCount}건`);
    console.log(`기대 최종:  ${baselineCount + TOTAL_ORDERS}건`);

    if (popularRes.status === 200) {
        const popular = popularRes.json().data || [];
        const found = popular.find(m => m.menuId === menuId);
        const finalCount = found?.orderCount;

        if (finalCount !== undefined) {
            const diff = finalCount - baselineCount;
            console.log(`실제 최종:  ${finalCount}건 (증가분: ${diff}건)`);

            if (diff === TOTAL_ORDERS) {
                console.log(`✅ [PASS] 정확히 ${TOTAL_ORDERS}건 누적 → 메시지 유실 없음`);
            } else if (diff < TOTAL_ORDERS) {
                console.error(`🚨 [FAIL] ${TOTAL_ORDERS - diff}건 유실`);
                console.error('   ▶ 원인 1: result 키 캐시 미만료');
                console.error(`     → redis-cli TTL "popular:menus:result"`);
                console.error(`     → redis-cli ZSCORE "popular:menus:$(date +%Y-%m-%d)" ${menuId}`);
                console.error('   ▶ 원인 2: DLT로 빠진 메시지');
                console.error('     → kafka-console-consumer --topic order-event.DLT');
                console.error('   ▶ 원인 3: Consumer Lag');
                console.error('     → kafka-consumer-groups --describe --group order-consumer-group');
            } else {
                console.error(`🚨 [FAIL] ${diff - TOTAL_ORDERS}건 중복 처리 → Consumer 멱등성 확인`);
            }
        } else {
            console.warn(`⚠️ ${menuName}이 TOP 3에 없음 → Redis CLI로 직접 확인 필요`);
            console.warn(`   redis-cli ZSCORE "popular:menus:$(date +%Y-%m-%d)" ${menuId}`);
        }
    }

    console.log('\n📌 엣지 케이스 체크리스트:');
    console.log('   1) kafka_order_failed > 0 → 409라면 DataInitializer 포인트 확인');
    console.log('   2) 카운트 미반영 → result 키 TTL이 남아있을 수 있음 (Redis CLI 직접 확인)');
    console.log('   3) 중복 카운트 → @KafkaListener retryOn/DLT 설정 확인');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}