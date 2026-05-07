// ====================================================================
// 인증 및 사용자 준비 헬퍼
// setup() 단계에서 호출하여 테스트용 유저/토큰 발급
// ====================================================================

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, HEADERS_JSON, authHeaders } from './config.js';

/**
 * 회원가입 (이미 존재하면 무시하고 진행)
 */
export function signup(email, password, nickname) {
    const res = http.post(
        `${BASE_URL}/api/auth/signup`,
        JSON.stringify({ email, password, nickname }),
        { headers: HEADERS_JSON, tags: { name: 'signup' } }
    );
    // 201 또는 409 (이미 존재) 모두 정상 처리
    return res.status === 201 || res.status === 409;
}

/**
 * 로그인 후 JWT 토큰 반환
 */
export function login(email, password) {
    const res = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password }),
        { headers: HEADERS_JSON, tags: { name: 'login' } }
    );

    if (res.status !== 200) {
        console.error(`[login 실패] ${email} status=${res.status} body=${res.body}`);
        return null;
    }
    const body = res.json();
    // 명세서: data.token 형태
    return body.data?.token || null;
}

/**
 * 회원가입 + 로그인을 한 번에 처리
 */
export function signupAndLogin(email, password, nickname) {
    signup(email, password, nickname);
    return login(email, password);
}

/**
 * 포인트 충전
 */
export function chargePoint(token, amount) {
    const res = http.post(
        `${BASE_URL}/api/points/me/charge`,
        JSON.stringify({ amount }),
        { headers: authHeaders(token), tags: { name: 'charge' } }
    );
    return {
        status: res.status,
        body: res.json(),
        duration: res.timings.duration,
    };
}

/**
 * 포인트 잔액 조회
 */
export function getPointBalance(token) {
    const res = http.get(
        `${BASE_URL}/api/points/me`,
        { headers: authHeaders(token), tags: { name: 'get_balance' } }
    );
    if (res.status !== 200) return -1;
    return res.json().data?.pointBalance ?? -1;
}

/**
 * 테스트 유저 일괄 생성
 * @param {number} count - 생성할 유저 수
 * @param {string} prefix - 이메일 prefix (e.g. 'order-test')
 * @param {number} initialPoint - 초기 충전 포인트 (0이면 충전 안 함)
 */
export function prepareUsers(count, prefix, initialPoint = 0) {
    const users = [];
    for (let i = 0; i < count; i++) {
        const email = `${prefix}-${i}@test.com`;
        const password = 'Password123!';
        const nickname = `${prefix}-${i}`;
        const token = signupAndLogin(email, password, nickname);

        if (!token) {
            console.error(`[prepareUsers] ${email} 토큰 발급 실패`);
            continue;
        }

        if (initialPoint > 0) {
            chargePoint(token, initialPoint);
        }

        users.push({ email, token, index: i });
    }
    console.log(`[prepareUsers] ${users.length}/${count} 유저 준비 완료`);
    return users;
}