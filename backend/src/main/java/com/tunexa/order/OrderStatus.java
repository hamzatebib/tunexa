package com.tunexa.order;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Statuts d'un colis (sous-commande) ; le statut de la commande est calculé à partir de ses colis. */
public enum OrderStatus {
    PENDING_CONFIRMATION, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, DELIVERY_FAILED, RETURNED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_CONFIRMATION, EnumSet.of(CONFIRMED, CANCELLED),
            CONFIRMED, EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING, EnumSet.of(SHIPPED, CANCELLED),
            SHIPPED, EnumSet.of(DELIVERED, DELIVERY_FAILED),
            DELIVERY_FAILED, EnumSet.of(SHIPPED, RETURNED),
            DELIVERED, EnumSet.noneOf(OrderStatus.class),
            RETURNED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class));

    public boolean canMoveTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    /** Le client peut encore annuler tant que le colis n'est pas remis au transporteur. */
    public boolean isCancellable() {
        return canMoveTo(CANCELLED);
    }
}
