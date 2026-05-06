package com.example.kserverproject.domain.order.repository;

import com.example.kserverproject.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 주문 단건 조회용
    @Query("""
             SELECT o FROM Order o
             JOIN FETCH o.orderItems oi
             JOIN FETCH oi.menu
             WHERE o.id = :orderId
            """)
    Optional<Order> findOrderWithItemsAndMenus(@Param("orderId")Long orderId);

    // 주문 목록 조회용
    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.orderItems oi
            JOIN FETCH oi.menu
            WHERE o.user.id = :userId
            """)
    Page<Order> findOrdersWithItemsAndMenus(@Param("userId")Long userId, Pageable pageable);
}
