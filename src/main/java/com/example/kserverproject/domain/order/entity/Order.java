package com.example.kserverproject.domain.order.entity;

import com.example.kserverproject.common.entity.BaseEntity;
import com.example.kserverproject.common.exception.OrderException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus orderStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    public Order(User user, Long totalAmount, OrderStatus orderStatus) {
        this.user = user;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
    }

    // 주문 완료
    public void completeOrder() {
        if (this.orderStatus != OrderStatus.CREATED) {
            throw new OrderException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.orderStatus = OrderStatus.COMPLETED;
    }

    // 주문 취소
    public void cancelOrder() {
        if (this.orderStatus != OrderStatus.COMPLETED) {
            throw new OrderException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.orderStatus = OrderStatus.CANCELED;
    }

    // 주문 생성 시 OrderItem을 Order에 추가하는 메서드
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }

}
