package com.example.kserverproject.domain.order.entity;

import com.example.kserverproject.common.entity.BaseEntity;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_name",
                        columnNames = {"order_id", "menu_id"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", nullable = false)
    private Long price;

    @Builder
    public OrderItem(Order order, Menu menu, int quantity, Long price) {
        this.order = order;
        this.menu = menu;
        this.quantity = quantity;
        this.price = price;
    }

    public void connectOrder(Order order) {
        this.order = order;
    }
}
