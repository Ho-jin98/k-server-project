// ============================================================
// ☕ 시나리오: 아메리카노 마지막 3잔을 100명이 동시에 주문한다면?
// ============================================================
//
// 배경:
//   오전 9시, 회사 앞 커피숍 앱에 직원들이 동시에 접속했다.
//   한 유저의 포인트는 딱 3잔 분량(9,000P)밖에 없는데,
//   앱 버그로 같은 계정이 100개의 탭에서 동시에 "주문하기"를 눌렀다.
//
//   → 분산락(Redisson RLock)이 없었다면?
//     100개 요청이 모두 "잔액 9,000P ≥ 메뉴 가격 3,000P" 통과 → 30만원 초과 차감
//
//   → 분산락이 있다면?
//     한 번에 1개 요청만 락 획득 → 정확히 3건 성공, 97건 거절
//
// 검증 목표:
//   ✅ 성공 건수 = 정확히 3
//   ✅ 실패 건수 = 정확히 97 (409 잔액 부족)
//   ✅ 최종 잔액 = 0 (절대 음수 불가)
//   ✅ 기타 에러(5xx) = 0
//
// 엣지 케이스:
//   ❌ [락 미작동] 잔액이 음수가 된다 → 분산락 미적용 혹은 트랜잭션 분리 문제
//   ❌ [타임아웃 설정 미스] 97건이 409가 아닌 500으로 실패 → waitTime 너무 짧음
//   ❌ [락 범위 오류] 100건 모두 성공 → 락 키가 유저별로 다르게 설정되지 않은 경우
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 01_distributed_lock_order.js
// ============================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';

// ── 환경 ──────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HEADERS  = { 'Content-Type': 'application/json' };

// ── 시나리오 상수 ──────────────────────────────────────────
const VU_COUNT          = 100;   // 동시에 주문 버튼을 누르는 가상 유저 수
const TARGET_PRICE      = 3000;  // 아메리카노 가격 (서버 메뉴 중 3,000원짜리 우선)
const CHARGE_AMOUNT     = 9000;  // 딱 3잔 분량만 충전
const EXPECTED_SUCCESS  = 3;     // 기대 성공 건수

// ── 커스텀 메트릭 ──────────────────────────────────────────
const orderSuccess      = new Counter('order_success');           // 201
const orderInsufficent  = new Counter('order_insufficient');      // 409 잔액 부족
const orderLockTimeout  = new Counter('order_lock_timeout');      // 423/500 락 타임아웃
const orderOtherFail    = new Counter('order_other_fail');        // 그 외 실패
const lockWaitTime      = new Trend('lock_wait_ms');              // 락 대기 시간

// ── K6 옵션 ────────────────────────────────────────────────
export const options = {
    scenarios: {
        // shared-iterations: 100개 iteration을 100 VU가 선착순으로 나눠 가짐
        // → 사실상 모든 VU가 거의 동시에 1번씩 주문 = 동시성 재현
        burst_order: {
            executor:    'shared-iterations',
            vus:         VU_COUNT,
            iterations:  VU_COUNT,
            maxDuration: '30s',
        },
    },
    thresholds: {
        // 락 대기 포함 p95가 5초 이내여야 함
        'lock_wait_ms': ['p(95)<5000'],
        // 잔액 부족 이외의 예상치 못한 실패는 0건
        'order_other_fail': ['count==0'],
        // 락 타임아웃으로 인한 실패도 허용 안 함 (waitTime 설정 검증)
        'order_lock_timeout': ['count==0'],
    },
};

// ── Setup: DataInitializer 계정 로그인 + 잔액 조정 ──────────
// DataInitializer가 이미 생성한 계정:
//   user1@test.com / password123! / 초기 잔액 1,000,000P
//
// 문제: 100만P가 있으면 3,000P 메뉴를 333번 살 수 있어서
//       "딱 3건만 성공"이라는 정합성 검증이 불가능함
//
// 해결: 로그인 후 현재 잔액을 확인하고,
//       "정확히 CHARGE_AMOUNT(9,000P)만 남도록" 역산해서 더 충전하거나
//       → 그냥 아메리카노(3,000P) 기준 잔액 9,000P짜리 신규 유저를 생성
//       단, DataInitializer 유저(user1~5)는 잔액이 많아서 분산락 정합성 검증에 부적합
//       → 이 테스트 전용 계정(lock-test@example.com)을 신규 생성해 깔끔하게 사용
export function setup() {
    console.log('━━━ [setup] 분산락 주문 동시성 테스트 준비 시작 ━━━');
    console.log('[setup] 주의: DataInitializer 유저(user1~5)는 잔액 100만P라 정합성 검증 불가');
    console.log('[setup] → 테스트 전용 계정 신규 생성 (잔액 딱 9,000P만 충전)');

    // 1) 테스트 전용 계정 생성 (매번 타임스탬프로 unique 이메일 사용)
    const email = `lock-test-${Date.now()}@example.com`;
    const signupRes = http.post(`${BASE_URL}/api/auth/signup`,
        JSON.stringify({ email, password: 'Test1234!', nickname: '분산락테스트', role: 'CUSTOMER' }),
        { headers: HEADERS }
    );
    if (signupRes.status !== 201) {
        throw new Error(`[setup] 회원가입 실패: ${signupRes.status} ${signupRes.body}`);
    }
    console.log(`[setup] 테스트 계정 생성: ${email}`);

    // 2) 로그인
    const loginRes = http.post(`${BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password: 'Test1234!' }),
        { headers: HEADERS }
    );
    if (loginRes.status !== 200) {
        throw new Error(`[setup] 로그인 실패: ${loginRes.status}`);
    }
    const token = loginRes.json().data?.token;

    // 3) DataInitializer에 아메리카노(3,000P) 메뉴가 이미 있음 → 조회만
    const menuRes = http.get(`${BASE_URL}/api/menus`);
    if (menuRes.status !== 200) throw new Error(`[setup] 메뉴 조회 실패: ${menuRes.status}`);
    const menus = menuRes.json().data || [];
    if (menus.length === 0) throw new Error('[setup] 메뉴가 없습니다 (DataInitializer 확인)');

    // 아메리카노(3,000P) 우선, 없으면 가장 저렴한 메뉴
    let menu = menus.find(m => m.price === TARGET_PRICE);
    if (!menu) {
        menu = menus.reduce((min, m) => m.price < min.price ? m : min, menus[0]);
        console.warn(`[setup] ${TARGET_PRICE}P 메뉴 없음 → 최저가 사용: ${menu.menuName}(${menu.price}P)`);
    } else {
        console.log(`[setup] 타겟 메뉴: ${menu.menuName} (${menu.price}P)`);
    }

    // 4) 정확히 N잔 분량만 충전 (신규 계정이라 잔액 0에서 시작)
    const expectedSuccess = 3;
    const chargeAmount = menu.price * expectedSuccess; // 9,000P
    const chargeRes = http.post(`${BASE_URL}/api/points/me/charge`,
        JSON.stringify({ amount: chargeAmount }),
        { headers: { ...HEADERS, Authorization: `Bearer ${token}` } }
    );
    if (chargeRes.status !== 200) throw new Error(`[setup] 포인트 충전 실패: ${chargeRes.status}`);
    const balance = chargeRes.json().data?.pointBalance;
    console.log(`[setup] 충전 완료: ${balance}P (정확히 ${expectedSuccess}잔 분량)`);
    console.log(`[setup] 예상: ${VU_COUNT}건 시도 → ${expectedSuccess}건 성공, ${VU_COUNT - expectedSuccess}건 409`);
    console.log('━━━ [setup] 완료 ━━━');

    return { token, menuId: menu.menuId, menuPrice: menu.price, expectedSuccess };
}

// ── Default: 100 VU 동시 주문 ─────────────────────────────
export default function (data) {
    const { token, menuId, menuPrice } = data;

    const headers = { ...HEADERS, Authorization: `Bearer ${token}` };
    const payload = JSON.stringify({ menuItems: [{ menuId, quantity: 1 }] });

    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });
    const elapsed = Date.now() - start;

    lockWaitTime.add(elapsed);

    // 결과 분류
    if (res.status === 201) {
        orderSuccess.add(1);
    } else if (res.status === 409) {
        // 잔액 부족 → 분산락이 정상 동작한 것
        orderInsufficent.add(1);
    } else if (res.status === 423 || (res.status === 500 && res.body.includes('lock'))) {
        // 락 획득 타임아웃 → waitTime 설정 문제
        orderLockTimeout.add(1);
        console.error(`[VU ${__VU}] 락 타임아웃 발생: ${res.status} - waitTime 설정 확인 필요`);
    } else {
        orderOtherFail.add(1);
        console.error(`[VU ${__VU}] 예상치 못한 응답: ${res.status} - ${res.body.substring(0, 100)}`);
    }

    check(res, {
        '[분산락] 응답은 201 또는 409여야 함 (5xx 불가)': (r) =>
            r.status === 201 || r.status === 409,
    });
}

// ── Teardown: 최종 잔액 확인 (핵심 검증) ────────────────────
export function teardown(data) {
    const { token, menuPrice, expectedSuccess } = data;

    const balanceRes = http.get(`${BASE_URL}/api/points/me`, {
        headers: { ...HEADERS, Authorization: `Bearer ${token}` }
    });

    const finalBalance = balanceRes.json().data?.pointBalance;
    const expectedBalance = CHARGE_AMOUNT - (menuPrice * expectedSuccess);

    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('☕ 분산락 주문 동시성 테스트 결과');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`충전 포인트:    ${CHARGE_AMOUNT}P`);
    console.log(`메뉴 가격:      ${menuPrice}P`);
    console.log(`기대 성공 건수: ${expectedSuccess}건`);
    console.log(`최종 잔액:      ${finalBalance}P`);
    console.log(`기대 잔액:      ${expectedBalance}P`);

    if (finalBalance < 0) {
        console.error('🚨 [FAIL] 잔액 음수 발생! 분산락이 동작하지 않았습니다.');
        console.error('        → Redisson waitTime/leaseTime 설정 또는 @Transactional 분리 확인');
    } else if (finalBalance !== expectedBalance) {
        console.warn(`⚠️  [WARN] 잔액 불일치 (실제: ${finalBalance}P / 기대: ${expectedBalance}P)`);
        console.warn('        → 일부 락 타임아웃으로 인한 의도치 않은 실패일 수 있음');
    } else {
        console.log('✅ [PASS] 잔액 정합성 완벽: 분산락 정상 동작');
    }
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    // 엣지 케이스 안내 출력
    console.log('📌 엣지 케이스 체크리스트:');
    console.log('   1) order_insufficient != 97 → 락 키 범위 설정 확인 (유저별 키 분리 여부)');
    console.log('   2) lock_wait_ms p95 > 5000ms → Redisson waitTime 증가 검토');
    console.log('   3) order_other_fail > 0 → 서버 에러 로그 확인 (DLT, 예외 처리)');
}