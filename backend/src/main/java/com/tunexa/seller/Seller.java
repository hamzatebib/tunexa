package com.tunexa.seller;

import java.math.BigDecimal;
import java.time.Instant;

import com.tunexa.geo.Governorate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sellers")
@Getter
@Setter
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private String shopName;
    private String legalName;
    /** Matricule fiscal. */
    private String taxId;
    private String rneNumber;
    private String rib;
    private String pickupAddress;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "governorate_id")
    private Governorate governorate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellerStatus status = SellerStatus.PENDING;

    private String rejectionReason;

    /** Commission Tunexa en points de base (1000 = 10 %). */
    private int commissionBp;

    private BigDecimal rating;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isApproved() {
        return status == SellerStatus.APPROVED;
    }
}
