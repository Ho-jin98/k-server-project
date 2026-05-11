# 📋 API 상세 명세서

---

## 1. 인증 (Auth)

### 회원가입

**`POST /api/auth/signup`**

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123!",
  "nickname": "호진"
}
```

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "호진",
    "role": "CUSTOMER"
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `400` | 유효하지 않은 요청값 |
| `409` | 이미 존재하는 이메일 |

---

### 로그인

**`POST /api/auth/login`**

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "grantType": "Bearer",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `400` | 유효하지 않은 요청값 |
| `401` | 이메일 또는 비밀번호 불일치 |

---

### 로그아웃

**`POST /api/auth/logout`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": null
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 유효하지 않은 토큰 |

---

## 2. 유저 (User)

### 내 정보 조회

**`GET /api/users/me`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "호진",
    "role": "CUSTOMER"
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `404` | 존재하지 않는 유저 |

---

## 3. 포인트 (Point)

### 포인트 잔액 조회

**`GET /api/points/me`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "pointBalance": 5000
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `404` | 존재하지 않는 유저 |

---

### 포인트 충전

**`POST /api/points/me/charge`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "amount": 5000
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "chargeAmount": 5000,
    "pointBalance": 10000
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `400` | 충전 금액이 0 이하인 경우 |
| `401` | 인증 토큰 없음 또는 만료 |
| `404` | 존재하지 않는 유저 |

---

### 포인트 거래 내역 조회

**`GET /api/points/me/histories`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Query Parameter**
```
page=0&size=20
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "historyId": 1,
        "amount": 5000,
        "pointType": "CHARGE",
        "balanceAfter": 10000,
        "createdAt": "2025-05-01T10:00:00"
      },
      {
        "historyId": 2,
        "amount": 3000,
        "pointType": "PAYMENT",
        "balanceAfter": 7000,
        "createdAt": "2025-05-01T11:00:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 2
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `404` | 존재하지 않는 유저 |

---

## 4. 메뉴 (Menu)

### 메뉴 목록 전체 조회

**`GET /api/menus`**

**Response `200 OK`**
```json
{
  "success": true,
  "data": [
    {
      "menuId": 1,
      "menuName": "아메리카노",
      "price": 3000,
      "imageUrl": "https://example.com/images/americano.jpg"
    }
  ]
}
```

---

### 메뉴 단건 조회

**`GET /api/menus/{menuId}`**

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "menuId": 1,
    "menuName": "아메리카노",
    "price": 3000,
    "imageUrl": "https://example.com/images/americano.jpg"
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `404` | 존재하지 않는 메뉴 |

---

### 인기 메뉴 TOP 3 조회

**`GET /api/menus/popular`**

**Response `200 OK`**
```json
{
  "success": true,
  "data": [
    {
      "rank": 1,
      "menuId": 1,
      "menuName": "아메리카노",
      "price": 3000,
      "imageUrl": "https://example.com/images/americano.jpg",
      "orderCount": 120
    }
  ]
}
```

---

### 메뉴 검색

**`GET /api/menus/search`**

**Query Parameter**
```
keyword=아메리카노&page=1&size=10
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "menuId": 1,
        "menuName": "아메리카노",
        "price": 3000,
        "imageUrl": "https://example.com/images/americano.jpg"
      }
    ],
    "page": 1,
    "size": 10,
    "totalElements": 1
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `400` | 검색어가 없는 경우 |

---

### 메뉴 등록 (ADMIN)

**`POST /api/admin/menus`**

**Header**
```
Authorization: Bearer {accessToken}  // ADMIN 권한
```

**Request Body**
```json
{
  "menuName": "바닐라라떼",
  "price": 4500,
  "imageUrl": "https://example.com/images/vanilla-latte.jpg"
}
```

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "menuId": 4,
    "menuName": "바닐라라떼",
    "price": 4500,
    "imageUrl": "https://example.com/images/vanilla-latte.jpg"
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `400` | 유효하지 않은 요청값 |
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | ADMIN 권한 없음 |
| `409` | 이미 존재하는 메뉴명 |

---

### 메뉴 수정 (ADMIN)

**`PATCH /api/admin/menus/{menuId}`**

**Header**
```
Authorization: Bearer {accessToken}  // ADMIN 권한
```

**Request Body**
```json
{
  "menuName": "바닐라라떼",
  "price": 5000,
  "imageUrl": "https://example.com/images/vanilla-latte.jpg"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "menuId": 4,
    "menuName": "바닐라라떼",
    "price": 5000,
    "imageUrl": "https://example.com/images/vanilla-latte.jpg"
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `400` | 유효하지 않은 요청값 |
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | ADMIN 권한 없음 |
| `404` | 존재하지 않는 메뉴 |

---

### 메뉴 삭제 (ADMIN)

**`DELETE /api/admin/menus/{menuId}`**

**Header**
```
Authorization: Bearer {accessToken}  // ADMIN 권한
```

**Response `204 No Content`**
```json
{
  "success": true,
  "data": null
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | ADMIN 권한 없음 |
| `404` | 존재하지 않는 메뉴 |

---

## 5. 주문 (Order)

### 주문 생성

**`POST /api/orders`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Request Body**
```json
{
  "menuItems": [
    { "menuId": 1, "quantity": 2 },
    { "menuId": 3, "quantity": 1 }
  ]
}
```

**Response `201 Created`**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "userId": 1,
    "orderItems": [
      {
        "menuId": 1,
        "menuName": "아메리카노",
        "quantity": 2,
        "price": 3000
      },
      {
        "menuId": 3,
        "menuName": "카푸치노",
        "quantity": 1,
        "price": 4500
      }
    ],
    "totalAmount": 10500,
    "orderStatus": "CREATED",
    "createdAt": "2025-05-01T10:00:00"
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `400` | 유효하지 않은 요청값 |
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | CUSTOMER 권한 없음 |
| `404` | 존재하지 않는 유저 또는 메뉴 |
| `409` | 포인트 잔액 부족 |

---

### 특정 주문 상세 조회

**`GET /api/orders/{orderId}`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "userId": 1,
    "orderItems": [
      {
        "menuId": 1,
        "menuName": "아메리카노",
        "quantity": 2,
        "price": 3000
      }
    ],
    "totalAmount": 6000,
    "orderStatus": "COMPLETED",
    "createdAt": "2025-05-01T10:00:00"
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | 본인 주문이 아닌 경우 |
| `404` | 존재하지 않는 주문 |

---

### 내 주문 목록 조회

**`GET /api/orders/me`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Query Parameter**
```
page=0&size=20
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": 1,
        "userId": 1,
        "orderItems": [
          {
            "menuId": 1,
            "menuName": "아메리카노",
            "quantity": 2,
            "price": 3000
          }
        ],
        "totalAmount": 6000,
        "orderStatus": "COMPLETED",
        "createdAt": "2025-05-01T10:00:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `404` | 존재하지 않는 유저 |

---

### 주문 취소

**`POST /api/orders/{orderId}/cancel`**

**Header**
```
Authorization: Bearer {accessToken}
```

**Response `200 OK`**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderStatus": "CANCELED",
    "refundedAmount": 3000,
    "pointBalance": 8000
  }
}
```

| 상태코드 | 설명 |
|----------|------|
| `401` | 인증 토큰 없음 또는 만료 |
| `403` | 본인 주문이 아닌 경우 |
| `404` | 존재하지 않는 주문 |
| `409` | 이미 취소된 주문 |

---

## 공통 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "존재하지 않는 유저입니다."
  }
}
```