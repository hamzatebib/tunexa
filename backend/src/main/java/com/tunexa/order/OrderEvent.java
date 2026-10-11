package com.tunexa.order;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Historique horodaté des changements de statut (commande ou colis). */
@Entity
@Table(name = "order_events")
@Getter
@Setter
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    private Long subOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String note;

    /** CUSTOMER, SELLER, ADMIN ou SYSTEM. */
    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
