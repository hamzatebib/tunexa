package com.tunexa.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private long itemsTotal;
    private long shippingTotal;
    private long stampDuty;
    private long codFee;
    private long total;

    private String shipFullName;
    private String shipPhone;
    private String shipGovernorate;
    private String shipDelegation;
    private String shipLocality;
    private String shipPostalCode;
    private String shipStreet;
    private String shipLandmark;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant confirmedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<SubOrder> subOrders = new ArrayList<>();

    /** Recalcule le statut global à partir des colis. */
    public void refreshStatus() {
        List<OrderStatus> statuses = subOrders.stream().map(SubOrder::getStatus).toList();
        List<OrderStatus> active = statuses.stream().filter(s -> s != OrderStatus.CANCELLED).toList();
        if (active.isEmpty()) {
            status = OrderStatus.CANCELLED;
        } else if (active.contains(OrderStatus.PENDING_CONFIRMATION)) {
            status = OrderStatus.PENDING_CONFIRMATION;
        } else if (active.stream().allMatch(s -> s == OrderStatus.DELIVERED)) {
            status = OrderStatus.DELIVERED;
        } else if (active.stream().allMatch(s -> s == OrderStatus.RETURNED)) {
            status = OrderStatus.RETURNED;
        } else if (active.stream().anyMatch(s -> s == OrderStatus.SHIPPED || s == OrderStatus.DELIVERED
                || s == OrderStatus.DELIVERY_FAILED || s == OrderStatus.RETURNED)) {
            status = OrderStatus.SHIPPED;
        } else if (active.contains(OrderStatus.PROCESSING)) {
            status = OrderStatus.PROCESSING;
        } else {
            status = OrderStatus.CONFIRMED;
        }
    }
}
