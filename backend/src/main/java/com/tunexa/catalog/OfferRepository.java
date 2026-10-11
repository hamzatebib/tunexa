package com.tunexa.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    @EntityGraph(attributePaths = {"seller"})
    List<Offer> findByProductId(Long productId);

    @EntityGraph(attributePaths = {"product", "product.brand"})
    List<Offer> findBySellerIdOrderByIdDesc(Long sellerId);

    Optional<Offer> findByIdAndSellerId(Long id, Long sellerId);

    boolean existsByProductIdAndSellerId(Long productId, Long sellerId);
}
