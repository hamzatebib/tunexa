package com.tunexa.order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByNumber(String number);

    Optional<Order> findByNumberAndUserId(String number, Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByNumber(String number);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant before);

    long countByCreatedAtAfter(Instant after);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.createdAt >= :after and o.status <> com.tunexa.order.OrderStatus.CANCELLED")
    long sumTotalSince(Instant after);
}
