package com.tunexa.seller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByUserId(Long userId);

    boolean existsByTaxIdIgnoreCase(String taxId);

    boolean existsByShopNameIgnoreCase(String shopName);

    List<Seller> findByStatusOrderByCreatedAtAsc(SellerStatus status);

    long countByStatus(SellerStatus status);
}
