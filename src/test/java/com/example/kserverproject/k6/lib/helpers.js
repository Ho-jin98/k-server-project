// ====================================================================
// API 호출 및 검증 헬퍼
// ====================================================================

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, HEADERS_JSON, authHeaders } from './config.js';

/**
 * 메뉴 목록 조회
 */
export function getMenus() {
    const res = http.get(`${BASE_URL}/api/menus`, {
        headers: HEADERS_JSON,
        tags: { name: 'get_menus' },
    });
    return res;
}

/**
 * 메뉴 상세 조회
 */
export function getMenuDetail(menuId) {
    return http.get(`${BASE_URL}/api/menus/${menuId}`, {
        headers: HEADERS_JSON,
        tags: { name: 'get_menu_detail' },
    });
}

/**
 * 메뉴 검색
 */
export function searchMenu(keyword) {
    return http.get(`${BASE_URL}/api/menus/search?keyword=${encodeURIComponent(keyword)}`, {
        headers: HEADERS_JSON,
        tags: { name: 'search_menu' },
    });
}

/**
 * 인기 메뉴 조회
 */
export function getPopularMenus() {
    return http.get(`${BASE_URL}/api/menus/popular`, {
        headers: HEADERS_JSON,
        tags: { name: 'get_popular_menus' },
    });
}

/**
 * 주문 생성
 * @param {string} token - JWT
 * @param {Array<{menuId, quantity}>} menuItems - 주문 아이템
 */
export function createOrder(token, menuItems) {
    const payload = JSON.stringify({ menuItems });
    const res = http.post(`${BASE_URL}/api/orders`, payload, {
        headers: authHeaders(token),
        tags: { name: 'create_order' },
    });
    return res;
}

/**
 * 주문 취소
 */
export function cancelOrder(token, orderId) {
    return http.post(
        `${BASE_URL}/api/orders/${orderId}/cancel`,
        null,
        { headers: authHeaders(token), tags: { name: 'cancel_order' } }
    );
}

/**
 * 응답에서 첫 번째 메뉴 ID 추출 (테스트용)
 */
export function getFirstMenuId() {
    const res = getMenus();
    if (res.status !== 200) return null;
    const menus = res.json().data;
    return Array.isArray(menus) && menus.length > 0 ? menus[0].menuId : null;
}

/**
 * 모든 메뉴 정보 반환
 */
export function getAllMenus() {
    const res = getMenus();
    if (res.status !== 200) return [];
    return res.json().data || [];
}