// ====================================================================
// K6 공통 설정
// 환경변수로 BASE_URL을 받지 않으면 localhost:8080 사용
// 실행 예시: k6 run -e BASE_URL=http://localhost:8080 script.js
// ====================================================================

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const HEADERS_JSON = {
    'Content-Type': 'application/json',
};

// 인증 헤더 빌더
export function authHeaders(token) {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };
}

// 공통 thresholds - 모든 시나리오에 권장 기준
export const COMMON_THRESHOLDS = {
    http_req_failed: ['rate<0.01'],         // 실패율 1% 미만
    http_req_duration: ['p(95)<2000'],      // p95 응답시간 2초 미만
};

// 동시성 시나리오 전용 thresholds (실패율 높을 수 있음 - 의도된 실패)
export const CONCURRENCY_THRESHOLDS = {
    http_req_duration: ['p(95)<5000'],      // 락 대기 시간 고려
};