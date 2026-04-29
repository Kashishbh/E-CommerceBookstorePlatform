package com.booknest.order.repository;

import com.booknest.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    Optional<Order> findFirstByOrderByOrderIdDesc();
    List<Order> findByOrderStatus(String orderStatus);
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
}
