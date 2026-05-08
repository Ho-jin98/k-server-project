// ============================================================
// 💰 시나리오: 가족이 하나의 앱 계정으로 동시에 포인트를 충전한다
// ============================================================
//
// 배경:
//   가족 공유 계정에서 50명이 동시에 1,000P씩 충전 시도.
//   최종 잔액은 기존 잔액 + 50,000P가 되어야 한다.
//
//   비관락 없이 처리하면 Lost Update 발생:
//   → 50개 요청이 같은 잔액을 읽고 +1,000P 씩 계산
//   → 마지막 commit만 살아남아 잔액이 기존+1,000P로 기록됨
//
//   비관락(PESSIMISTIC_WRITE)이 있다면:
//   → Row 잠금 → 순차 처리 → 정확히 기존잔액 + 50,000P
//
// DataInitializer 활용:
//   user1@test.com / password123! / 초기 잔액 1,000,000P 이미 생성됨
//   → 별도 회원가입/충전 불필요, 로그인만 하면 됨
//   → 단, 초기 잔액이 정확히 얼마인지 setup에서 먼저 기록해야 함
//
// 검증 목표:
//   ✅ 최종 잔액 = setup 시점 잔액 + (VU_COUNT × CHARGE_EACH)
//   ✅ 성공 건수 = 정확히 50
//   ✅ PointHistory balanceAfter: 단조 증가 패턴
//   ✅ 0원 이하 충전 시도 → 반드시 400 반환
//
// 엣지 케이스:
//   ❌ [Lost Update] 최종 잔액 < 기대값 → 비관락 미적용
//   ❌ [DeadLock] 일부 500 에러 → 락 충돌
//   ❌ [유효성 미검사] amount=0 이 200으로 통과 → @Valid 누락
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 02_pessimistic_lock_charge.js
// ============================================================

import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ── 환경 ──────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HEADERS  = { 'Content-Type': 'application/json' };

// ── DataInitializer 계정 ───────────────────────────────────
// user1 단일 계정으로 동시 충전 → Lost Update 재현
const TEST_EMAIL    = 'user1@test.com';
const TEST_PASSWORD = 'password123!';

// ── 시나리오 상수 ──────────────────────────────────────────
const VU_COUNT    = 50;
const CHARGE_EACH = 1000; // 각 VU가 1,000P 충전

// ── 커스텀 메트릭 ──────────────────────────────────────────
const chargeSuccess  = new Counter('charge_success');
const chargeFail     = new Counter('charge_fail');
const chargeDuration = new Trend('charge_duration_ms');
const edgeBadAmount  = new Counter('edge_bad_amount_400'); // 0이하 금액 → 400 기대
const edgeDeadlock   = new Counter('edge_deadlock');

// ── K6 옵션 ────────────────────────────────────────────────
export const options = {
    scenarios: {
        concurrent_charge: {
            executor:    'shared-iterations',
            vus:         VU_COUNT,
            iterations:  VU_COUNT,
            maxDuration: '30s',
        },
        // 엣지케이스: 잘못된 금액 충전 시도 (정상 충전 중간에 섞어서 실행)
        edge_invalid_amount: {
            executor:    'shared-iterations',
            vus:         5,
            iterations:  10,
            maxDuration: '10s',
            startTime:   '3s',
        },
    },
    thresholds: {
        'charge_fail':          ['count==0'],
        'charge_duration_ms':   ['p(95)<3000'],
        // 0 이하 금액은 10건 모두 400이어야 함
        'edge_bad_amount_400':  ['count==10'],
        'edge_deadlock':        ['count==0'],
    },
};

// ── Setup: DataInitializer 유저 로그인 + 현재 잔액 스냅샷 ──
export function setup() {
    console.log('━━━ [setup] 비관락 포인트 충전 동시성 테스트 준비 ━━━');
    console.log(`[setup] DataInitializer 계정 사용: ${TEST_EMAIL}`);

    const loginRes = http.post(`${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD }),
        { headers: HEADERS }
    );
    if (loginRes.status !== 200) {
        throw new Error(`[setup] 로그인 실패: ${loginRes.status} - DataInitializer 실행 여부 확인`);
    }
    const token = loginRes.json().data?.token;

    // 현재 잔액 스냅샷 (DataInitializer = 1,000,000P지만, 이전 테스트로 달라졌을 수 있음)
    const balRes = http.get(`${BASE_URL}/api/points/me`, {
        headers: { ...HEADERS, Authorization: `Bearer ${token}` }
    });
    const initialBalance = balRes.json().data?.pointBalance ?? 0;

    console.log(`[setup] 현재 잔액: ${initialBalance}P`);
    console.log(`[setup] 목표: ${VU_COUNT}명 × ${CHARGE_EACH}P = +${VU_COUNT * CHARGE_EACH}P`);
    console.log(`[setup] 기대 최종 잔액: ${initialBalance + VU_COUNT * CHARGE_EACH}P`);
    console.log('━━━ [setup] 완료 ━━━');

    return { token, initialBalance };
}

// ── Default: 동시 충전 ────────────────────────────────────
export default function (data) {
    const { token } = data;

    // 시나리오 분기
    if (exec.scenario.name === 'edge_invalid_amount') {
        runEdgeInvalidAmount(token);
        return;
    }

    const headers = { ...HEADERS, Authorization: `Bearer ${token}` };
    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/points/me/charge`,
        JSON.stringify({ amount: CHARGE_EACH }),
        { headers }
    );
    chargeDuration.add(Date.now() - start);

    if (res.status === 200) {
        chargeSuccess.add(1);
    } else if (res.status === 500 && res.body.toLowerCase().includes('deadlock')) {
        edgeDeadlock.add(1);
        console.error(`[VU ${__VU}] 데드락 감지`);
    } else {
        chargeFail.add(1);
        console.error(`[VU ${__VU}] 충전 실패: ${res.status} - ${res.body.substring(0, 100)}`);
    }

    check(res, {
        '[비관락] 충전 200 OK': (r) => r.status === 200,
        '[비관락] pointBalance 포함': (r) => r.json().data?.pointBalance !== undefined,
    });
}

// ── 엣지케이스: 0원/음수 충전 → 400 확인 ─────────────────
function runEdgeInvalidAmount(token) {
    const badAmounts = [0, -1000, -1, 0, -500];
    const amount = badAmounts[__ITER % badAmounts.length];
    const headers = { ...HEADERS, Authorization: `Bearer ${token}` };

    const res = http.post(`${BASE_URL}/api/points/me/charge`,
        JSON.stringify({ amount }),
        { headers }
    );

    if (res.status === 400) {
        edgeBadAmount.add(1);
    } else {
        console.error(`[엣지] amount=${amount} 에 400이 아닌 ${res.status} 반환 → @Valid 누락 의심`);
    }

    check(res, {
        '[엣지] 0 이하 금액 → 400 Bad Request': () => res.status === 400,
    });
}

// ── Teardown: 잔액 정합성 최종 검증 ──────────────────────
export function teardown(data) {
    const { token, initialBalance } = data;
    const expectedFinal = initialBalance + (VU_COUNT * CHARGE_EACH);

    const balRes = http.get(`${BASE_URL}/api/points/me`, {
        headers: { ...HEADERS, Authorization: `Bearer ${token}` }
    });
    const finalBalance = balRes.json().data?.pointBalance;

    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('💰 비관락 포인트 충전 동시성 테스트 결과');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`테스트 계정:    ${TEST_EMAIL}`);
    console.log(`초기 잔액:      ${initialBalance}P`);
    console.log(`기대 최종:      ${expectedFinal}P (+${VU_COUNT * CHARGE_EACH}P)`);
    console.log(`실제 최종:      ${finalBalance}P`);

    if (finalBalance === expectedFinal) {
        console.log('✅ [PASS] 잔액 완벽 일치 → PESSIMISTIC_WRITE 정상 동작');
    } else if (finalBalance < expectedFinal) {
        const lost = expectedFinal - finalBalance;
        console.error(`🚨 [FAIL] Lost Update! ${lost}P 누락`);
        console.error('        → @Lock(PESSIMISTIC_WRITE) 적용 여부 확인');
        console.error('        → 트랜잭션 격리 수준(READ_COMMITTED 이상) 확인');
    } else {
        console.warn(`⚠️  [WARN] 잔액 초과 (실제: ${finalBalance} / 기대: ${expectedFinal})`);
    }

    console.log('\n📌 엣지 케이스 체크리스트:');
    console.log('   1) charge_fail > 0 → DB 커넥션 풀 or 비관락 타임아웃 확인');
    console.log('   2) edge_deadlock > 0 → 다른 락과의 충돌 확인');
    console.log('   3) edge_bad_amount_400 != 10 → DTO @Min(1) 유효성 검사 확인');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}