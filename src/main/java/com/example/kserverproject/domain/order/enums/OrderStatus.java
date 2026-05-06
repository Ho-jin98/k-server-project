package com.example.kserverproject.domain.order.enums;

public enum OrderStatus {

    CREATED,    // 주문 생성 (결제 대기 상태)
    COMPLETED,  // 주문 완료 (결제 완료 상태)
    CANCELED,   // 주문 취소 (결제 완료 상태에서만 가능)
}
