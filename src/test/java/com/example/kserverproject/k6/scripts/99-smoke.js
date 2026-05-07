// 전체 플로우 스모크 테스트
// ====================================================================
// 99. 스모크 테스트 (전체 플로우 동작 확인)
// ====================================================================
// 목적:
//   - 본격적인 부하 테스트 전에 모든 API가 정상 동작하는지 빠르게 확인
//   - 로그인 → 충전 → 메뉴 조회 → 주문 → 인기메뉴 조회 → 취소
//
// 사전 조건:
//   - initData로 user1@test.com / Password123! 계정이 미리 생성되어 있어야 함
//
// 실행:
//   k6 run -e BASE_URL=http://localhost:8080 scripts/99-smoke.js
// ====================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, HEADERS_JSON, authHeaders } from '../lib/config.js';

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: {
        'http_req_failed': ['rate==0'],
    },
};

export default function () {
    const email = 'user1@test.com';
    const password = 'password123!';

    // 1. 로그인 (initData 계정 사용)
    let res = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password }),
        { headers: HEADERS_JSON }
    );

    console.log(`STATUS: ${res.status}`);
    console.log(`BODY: ${res.body}`);

    check(res, { '1. 로그인 (200)': (r) => r.status === 200 });
    console.log(`[1] 로그인: ${res.status}, body=${res.body}`);

    if (res.status !== 200) {
        console.error('로그인 실패, 이후 테스트 중단');
        return;
    }

    const token = res.json().data?.token;
    console.log(`    token=${token?.substring(0, 20)}...`);

    // 2. 내 정보 조회
    res = http.get(`${BASE_URL}/api/users/me`, { headers: authHeaders(token) });
    check(res, { '2. 내 정보 조회 (200)': (r) => r.status === 200 });
    console.log(`[2] 내 정보: ${res.status}`);

    // 3. 포인트 충전
    res = http.post(
        `${BASE_URL}/api/points/me/charge`,
        JSON.stringify({ amount: 50000 }),
        { headers: authHeaders(token) }
    );
    check(res, { '3. 포인트 충전 (200)': (r) => r.status === 200 });
    console.log(`[3] 충전: ${res.status}, 잔액=${res.json().data?.pointBalance}`);

    // 4. 메뉴 목록 조회
    res = http.get(`${BASE_URL}/api/menus`);
    check(res, {
        '4. 메뉴 목록 조회 (200)': (r) => r.status === 200,
        '   메뉴 1개 이상 존재': (r) => Array.isArray(r.json().data) && r.json().data.length > 0,
    });
    const menus = res.json().data || [];
    console.log(`[4] 메뉴 조회: ${res.status}, 메뉴 ${menus.length}개`);

    if (menus.length === 0) {
        console.error('메뉴가 없어 주문 테스트를 건너뜁니다.');
        return;
    }

    const targetMenu = menus[0];

    // 5. 메뉴 상세 조회
    res = http.get(`${BASE_URL}/api/menus/${targetMenu.menuId}`);
    check(res, { '5. 메뉴 상세 조회 (200)': (r) => r.status === 200 });
    console.log(`[5] 메뉴 상세: ${res.status}, 메뉴=${targetMenu.menuName}`);

    // 6. 주문 생성
    res = http.post(
        `${BASE_URL}/api/orders`,
        JSON.stringify({ menuItems: [{ menuId: targetMenu.menuId, quantity: 1 }] }),
        { headers: authHeaders(token) }
    );
    check(res, { '6. 주문 생성 (201)': (r) => r.status === 201 });
    const orderId = res.json().data?.orderId;
    console.log(`[6] 주문: ${res.status}, orderId=${orderId}`);

    if (!orderId) {
        console.error('주문 생성 실패, 이후 테스트 중단');
        return;
    }

    // 7. Kafka Consumer 처리 대기
    sleep(3);

    // 8. 내 주문 조회
    res = http.get(`${BASE_URL}/api/orders/${orderId}`, { headers: authHeaders(token) });
    check(res, { '8. 주문 상세 조회 (200)': (r) => r.status === 200 });
    console.log(`[8] 주문 상세: ${res.status}, status=${res.json().data?.orderStatus}`);

    // 9. 인기 메뉴 조회
    res = http.get(`${BASE_URL}/api/menus/popular`);
    check(res, { '9. 인기메뉴 조회 (200)': (r) => r.status === 200 });
    console.log(`[9] 인기메뉴: ${res.status}, ${(res.json().data || []).length}개`);

    // 10. 주문 취소
    res = http.post(
        `${BASE_URL}/api/orders/${orderId}/cancel`,
        null,
        { headers: authHeaders(token) }
    );
    check(res, { '10. 주문 취소 (200)': (r) => r.status === 200 });
    console.log(`[10] 주문 취소: ${res.status}, 환불=${res.json().data?.refundedAmount}P`);

    console.log('===========================================');
    console.log('스모크 테스트 완료 - 모든 API 정상 동작');
    console.log('===========================================');
}