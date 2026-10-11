package com.tunexa.order;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {

    @EntityGraph(attributePaths = {"order"})
    Page<SubOrder> findBySellerIdAndStatusInOrderByIdDesc(Long sellerId, Collection<OrderStatus> statuses,
            Pageable pageable);

    Optional<SubOrder> findByIdAndSellerId(Long id, Long sellerId);

    long countByStatus(OrderStatus status);
}
