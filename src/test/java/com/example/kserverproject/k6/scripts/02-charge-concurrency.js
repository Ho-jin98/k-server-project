// 포인트 충전 동시성 (비관락)
// ====================================================================
// 02. 포인트 충전 동시성 테스트 (DB 비관락 PESSIMISTIC_WRITE 검증)
// ====================================================================
// 목적:
//   - 동일 유저에게 동시에 충전 요청이 몰릴 때
//     DB 비관락이 Lost Update를 방지하는지 검증
//   - balanceAfter가 누적 일관성을 가지는지 (회계 무결성)
//
// 시나리오:
//   - 유저 1명 생성 (잔액 0)
//   - 20 VU가 각자 1,000P씩 동시 충전
//   - 기대 최종 잔액: 정확히 20,000P
//   - PointHistory의 balanceAfter는 1000, 2000, 3000... 순차 증가 패턴
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 scripts/02-charge-concurrency.js
// ====================================================================

import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { CONCURRENCY_THRESHOLDS } from '../lib/config.js';
import { signupAndLogin, chargePoint, getPointBalance } from '../lib/auth.js';

// ============== 커스텀 메트릭 ==============
const chargeSuccess = new Counter('charge_success_count');
const chargeFailed = new Counter('charge_failed_count');
const chargeDuration = new Trend('charge_duration');

// ============== 테스트 설정 ==============
const VU_COUNT = 20;
const CHARGE_AMOUNT = 1000;
const EXPECTED_FINAL_BALANCE = VU_COUNT * CHARGE_AMOUNT; // 20,000P

export const options = {
    scenarios: {
        concurrent_charges: {
            executor: 'shared-iterations',
            vus: VU_COUNT,
            iterations: VU_COUNT,
            maxDuration: '30s',
        },
    },
    thresholds: {
        ...CONCURRENCY_THRESHOLDS,
        'charge_success_count': [`count==${VU_COUNT}`], // 모든 충전이 성공해야 함
        'charge_failed_count': ['count==0'],
    },
};

// ============== setup ==============
export function setup() {
    const email = 'user1@test.com';
    const token = signupAndLogin(email, 'password123!', 'charge-test');
    if (!token) throw new Error('유저 생성 실패');

    const initialBalance = getPointBalance(token);
    console.log(`[setup] 유저: ${email}, 초기 잔액: ${initialBalance}P`);

    return { token, initialBalance };
}

// ============== default: 동시 충전 ==============
export default function (data) {
    const start = Date.now();
    const result = chargePoint(data.token, CHARGE_AMOUNT);
    const duration = Date.now() - start;
    chargeDuration.add(duration);

    if (result.status === 200) {
        chargeSuccess.add(1);
        check(result, {
            '충전 성공 (200)': () => result.status === 200,
            'pointBalance 증가': () => result.body?.data?.pointBalance > 0,
        });
    } else {
        chargeFailed.add(1);
        console.error(`[VU ${__VU}] 충전 실패 status=${result.status}, body=${JSON.stringify(result.body)}`);
    }
}

// ============== teardown: 최종 잔액 검증 ==============
export function teardown(data) {
    const finalBalance = getPointBalance(data.token);
    const expected = data.initialBalance + EXPECTED_FINAL_BALANCE;

    console.log('===========================================');
    console.log(`초기 잔액: ${data.initialBalance}P`);
    console.log(`기대 잔액: ${expected}P (${VU_COUNT}회 × ${CHARGE_AMOUNT}P)`);
    console.log(`실제 잔액: ${finalBalance}P`);
    console.log('===========================================');

    if (finalBalance === expected) {
        console.log('✅ [성공] 동시 충전 시에도 잔액 정확 → 비관락 정상 동작');
    } else if (finalBalance < expected) {
        console.error(`🚨 [Lost Update 발생] 차이: ${expected - finalBalance}P`);
        console.error('   → 비관락이 동작하지 않거나 트랜잭션 격리 문제');
    } else {
        console.error(`⚠️ 예상보다 많이 충전됨 (${finalBalance - expected}P 초과)`);
    }
}