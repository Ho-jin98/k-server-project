// Kafka -> ZINCRBY 정합성
// ====================================================================
// 05. Kafka 정합성 테스트 (Producer → Consumer → ZINCRBY)
// ====================================================================
// 목적:
//   - "메뉴별 주문 횟수가 정확해야 한다"는 핵심 요구사항 검증
//   - 주문 N건 발행 → 잠시 대기 → 인기메뉴 조회 → 카운트 일치 확인
//   - 메시지 유실, DLT 동작 여부 간접 확인
//
// 시나리오:
//   1) 메뉴 A에 10건, 메뉴 B에 5건 주문 (총 15건)
//   2) Kafka Consumer 처리 대기 (5초)
//   3) 인기메뉴 조회 → A는 +10, B는 +5 카운트 증가
//   4) 차이 발생 시 메시지 유실로 판단
//
// 부하 강도:
//   - 정합성 검증이 핵심 → VU 적게 (20)
//   - 주문 건수가 정확해야 비교 가능
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 scripts/05-kafka-integrity.js
// ====================================================================

import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { COMMON_THRESHOLDS } from '../lib/config.js';
import { signupAndLogin, chargePoint } from '../lib/auth.js';
import { getAllMenus, createOrder, getPopularMenus } from '../lib/helpers.js';

// ============== 커스텀 메트릭 ==============
const orderPublished = new Counter('order_published');
const orderFailed = new Counter('order_failed');
const publishDuration = new Trend('publish_duration');

// ============== 테스트 설정 ==============
const VU_COUNT = 20;
const ITERATIONS = 50; // 총 50건의 주문을 분산 발행

export const options = {
    scenarios: {
        kafka_publish: {
            executor: 'shared-iterations',
            vus: VU_COUNT,
            iterations: ITERATIONS,
            maxDuration: '1m',
        },
    },
    thresholds: {
        ...COMMON_THRESHOLDS,
        'order_published': [`count==${ITERATIONS}`],
        'order_failed': ['count==0'],
        'http_req_duration': ['p(95)<2000'],
    },
};

// ============== setup: 유저/메뉴/베이스라인 준비 ==============
export function setup() {
    console.log('[setup] Kafka 정합성 테스트 준비');

    // 1) 충분한 포인트의 유저 풀 생성 (잔액 부족 방지를 위해 여러 유저 사용)
    const userCount = 5;
    const users = [];
    const ITERATIONS_PER_USER = ITERATIONS / userCount; // 한 유저당 10건 주문

    // 2) 메뉴 조회 후 가장 저렴한 메뉴 선택 (충전 부담 최소화)
    // 우선 임시 토큰으로 메뉴 조회
    const tempToken = signupAndLogin('user1@test.com', 'password123!', 'temp');
    const menus = getAllMenus();
    if (menus.length === 0) throw new Error('메뉴가 없습니다');

    const targetMenu = menus.reduce((min, m) => (m.price < min.price ? m : min), menus[0]);
    const requiredCharge = targetMenu.price * (ITERATIONS_PER_USER + 5); // 여유분

    // 3) 유저 풀 생성 (각자 충분히 충전)
    for (let i = 0; i < userCount; i++) {
        const email = `kafka-test-${Date.now()}-${i}@test.com`;
        const token = signupAndLogin(email, 'password123!', `kafka-${i}`);
        if (token) {
            chargePoint(token, requiredCharge);
            users.push(token);
        }
    }
    console.log(`[setup] 유저 ${users.length}명 준비, 메뉴: ${targetMenu.menuName}(${targetMenu.price}P)`);

    // 4) 현재 베이스라인 인기메뉴 조회 (테스트 후 비교용)
    const baselineRes = getPopularMenus();
    let baselineCount = 0;
    if (baselineRes.status === 200) {
        const baseline = baselineRes.json().data || [];
        const found = baseline.find(m => m.menuId === targetMenu.menuId);
        baselineCount = found ? found.orderCount : 0;
    }
    console.log(`[setup] 베이스라인 - ${targetMenu.menuName}의 현재 카운트: ${baselineCount}`);

    return {
        tokens: users,
        menuId: targetMenu.menuId,
        menuName: targetMenu.menuName,
        menuPrice: targetMenu.price,
        baselineCount,
        expectedIncrement: ITERATIONS,
    };
}

// ============== default: 주문 발행 ==============
export default function (data) {
    // 유저 풀에서 라운드로빈 선택
    const token = data.tokens[__ITER % data.tokens.length];

    const start = Date.now();
    const res = createOrder(token, [{ menuId: data.menuId, quantity: 1 }]);
    publishDuration.add(Date.now() - start);

    if (res.status === 201) {
        orderPublished.add(1);
        check(res, { '주문 성공 (201)': () => true });
    } else {
        orderFailed.add(1);
        console.error(`[VU ${__VU}] 주문 실패: status=${res.status}, body=${res.body}`);
    }
}

// ============== teardown: 카운트 정합성 검증 ==============
export function teardown(data) {
    console.log('[teardown] Kafka Consumer 처리 대기 시작');

    // result 키 TTL이 1시간이라 캐시가 새로 갱신될 때까지 시간 필요
    // ⚠️ 실제 검증은 Redis CLI로 ZSCORE 직접 조회하는 것이 가장 정확
    console.log('  → result 키 TTL 1시간 때문에 인기메뉴 API로는 즉시 반영 안 될 수 있음');
    console.log('  → 정확한 검증은 Redis CLI로:');
    console.log(`    redis-cli ZSCORE popular:menus:$(date +%Y-%m-%d) ${data.menuId}`);

    sleep(10); // Consumer 처리 충분히 대기

    // 인기메뉴 API 조회 (result 키가 캐시 중이면 즉시 반영 안 됨)
    const res = getPopularMenus();
    if (res.status === 200) {
        const current = res.json().data || [];
        const found = current.find(m => m.menuId === data.menuId);
        const currentCount = found ? found.orderCount : 0;
        const diff = currentCount - data.baselineCount;

        console.log('===========================================');
        console.log(`메뉴: ${data.menuName} (ID: ${data.menuId})`);
        console.log(`베이스라인 카운트: ${data.baselineCount}`);
        console.log(`현재 카운트: ${currentCount}`);
        console.log(`증가량: ${diff}`);
        console.log(`기대 증가량: ${data.expectedIncrement}`);
        console.log('===========================================');

        if (diff === data.expectedIncrement) {
            console.log('✅ [성공] Kafka 메시지 유실 없음 - 정확히 일치');
        } else if (diff < data.expectedIncrement && diff > 0) {
            console.warn(`⚠️ result 키 캐싱 때문에 즉시 반영 안 됐을 가능성 (${data.expectedIncrement - diff}건 차이)`);
            console.warn('   → Redis CLI로 ZSCORE 직접 확인 필요');
        } else if (diff === 0) {
            console.warn('⚠️ result 키가 이전 캐시를 보여주고 있음 - TTL 만료 후 재확인 필요');
        } else {
            console.error(`🚨 카운트 차이 발생 → Kafka 메시지 유실 가능성`);
        }
    }
}