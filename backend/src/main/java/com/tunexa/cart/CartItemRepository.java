package com.tunexa.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {"offer", "offer.product", "offer.product.brand", "offer.seller"})
    List<CartItem> findByUserIdOrderByAddedAtAsc(Long userId);

    Optional<CartItem> findByUserIdAndOfferId(Long userId, Long offerId);

    void deleteByUserId(Long userId);
}
