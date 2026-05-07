// 주문 동시성 (Redisson 분산락)
// ====================================================================
// 01. 주문 동시성 테스트 (Redisson 분산락 검증)
// ====================================================================
// 목적:
//   - 동일 유저에게 동시에 주문 요청이 몰릴 때
//     Redisson 분산락이 정상 동작하여 잔액 초과 차감을 방지하는지 검증
//
// 시나리오:
//   - 유저 1명에게 정확히 9,000P 충전 (3,000P 메뉴 3건 분량)
//   - 30 VU가 동시에 같은 메뉴 1잔씩 주문 시도
//   - 기대: 정확히 3건만 201 성공, 27건은 409(잔액 부족)
//   - 최종 잔액: 0 (음수 절대 X)
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 scripts/01-order-concurrency.js
// ====================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, authHeaders, CONCURRENCY_THRESHOLDS } from '../lib/config.js';
import { signupAndLogin, chargePoint, getPointBalance } from '../lib/auth.js';
import { getFirstMenuId, getAllMenus, createOrder } from '../lib/helpers.js';

// ============== 커스텀 메트릭 ==============
const orderSuccess = new Counter('order_success_count');     // 201 성공
const orderFailedInsufficient = new Counter('order_failed_insufficient'); // 409 잔액 부족
const orderFailedOther = new Counter('order_failed_other');  // 기타 실패
const lockWaitDuration = new Trend('lock_wait_duration');    // 락 대기 시간

// ============== 테스트 설정 ==============
const VU_COUNT = 30;
const MENU_PRICE_TARGET = 3000; // 3,000원짜리 메뉴를 사용 (잔액 9,000으로 정확히 3잔만 가능)
const EXPECTED_ORDERS_PER_USER = 3;

export const options = {
    scenarios: {
        concurrent_orders: {
            executor: 'shared-iterations',
            vus: VU_COUNT,
            iterations: VU_COUNT, // 한 VU당 1회만 실행 (총 30회 시도)
            maxDuration: '30s',
        },
    },
    thresholds: {
        ...CONCURRENCY_THRESHOLDS,
        // 정확히 3건 성공해야 함
        'order_success_count': [`count==${EXPECTED_ORDERS_PER_USER}`],
        // 나머지는 모두 잔액 부족이어야 함
        'order_failed_insufficient': [`count==${VU_COUNT - EXPECTED_ORDERS_PER_USER}`],
        'order_failed_other': ['count==0'],
    },
};

// ============== setup: 유저/메뉴 준비 ==============
export function setup() {
    console.log('[setup] 시작');

    // 1) 테스트 유저 1명 생성 (한 유저에 동시 주문)
    const email = 'user1@test.com';
    const token = signupAndLogin(email, 'password123!', 'order-test');
    if (!token) throw new Error('유저 생성 실패');

    // 2) 메뉴 조회 → 가격 3,000원짜리 또는 가장 저렴한 메뉴 선택
    const menus = getAllMenus();
    if (menus.length === 0) {
        throw new Error('메뉴가 없습니다. 먼저 ADMIN으로 메뉴를 등록해주세요.');
    }

    // 가격이 정확히 MENU_PRICE_TARGET인 메뉴를 우선, 없으면 가장 저렴한 메뉴
    let targetMenu = menus.find(m => m.price === MENU_PRICE_TARGET);
    if (!targetMenu) {
        targetMenu = menus.reduce((min, m) => (m.price < min.price ? m : min), menus[0]);
        console.warn(`[setup] ${MENU_PRICE_TARGET}원 메뉴 없음 → ${targetMenu.menuName}(${targetMenu.price}원) 사용`);
    }

    // 3) 정확히 3잔 분량만 충전 (3,000원 메뉴면 9,000P 충전)
    const chargeAmount = targetMenu.price * EXPECTED_ORDERS_PER_USER;
    const chargeResult = chargePoint(token, chargeAmount);
    if (chargeResult.status !== 200) {
        throw new Error(`충전 실패: ${JSON.stringify(chargeResult.body)}`);
    }

    const balance = getPointBalance(token);
    console.log(`[setup] 완료 - 유저: ${email}, 메뉴: ${targetMenu.menuName}(${targetMenu.price}P), 잔액: ${balance}P`);

    return {
        token,
        menuId: targetMenu.menuId,
        menuPrice: targetMenu.price,
        initialBalance: balance,
    };
}

// ============== default: 동시 주문 실행 ==============
export default function (data) {
    const start = Date.now();

    const res = createOrder(data.token, [
        { menuId: data.menuId, quantity: 1 },
    ]);

    const duration = Date.now() - start;
    lockWaitDuration.add(duration);

    if (res.status === 201) {
        orderSuccess.add(1);
        check(res, { '주문 성공 (201)': (r) => r.status === 201 });
    } else if (res.status === 409) {
        orderFailedInsufficient.add(1);
        check(res, { '잔액 부족 (409)': (r) => r.status === 409 });
    } else {
        orderFailedOther.add(1);
        console.error(`[VU ${__VU}] 예상치 못한 응답: status=${res.status}, body=${res.body}`);
    }
}

// ============== teardown: 최종 잔액 검증 ==============
export function teardown(data) {
    const finalBalance = getPointBalance(data.token);
    console.log('===========================================');
    console.log(`초기 잔액: ${data.initialBalance}P`);
    console.log(`최종 잔액: ${finalBalance}P`);
    console.log(`기대 잔액: 0P (정확히 ${EXPECTED_ORDERS_PER_USER}건만 성공해야 함)`);
    console.log('===========================================');

    if (finalBalance < 0) {
        console.error('🚨 [정합성 위반] 잔액이 음수! 분산락 동작 실패');
    } else if (finalBalance === 0) {
        console.log('✅ [성공] 정확히 잔액 0 → 분산락 정상 동작');
    } else {
        console.warn(`⚠️ 잔액 ${finalBalance}P 남음 - 일부 주문이 락 대기 중 타임아웃 가능성`);
    }
}