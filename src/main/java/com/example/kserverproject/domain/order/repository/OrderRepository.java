package com.example.kserverproject.domain.order.repository;

import com.example.kserverproject.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
