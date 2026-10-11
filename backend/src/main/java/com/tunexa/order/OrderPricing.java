package com.tunexa.order;

import org.springframework.stereotype.Component;

import com.tunexa.config.TunexaProperties;

/** Règles de calcul des frais : livraison par zone et par colis, droit de timbre, frais COD. */
@Component
public class OrderPricing {

    private final TunexaProperties.Orders config;

    public OrderPricing(TunexaProperties properties) {
        this.config = properties.orders();
    }

    public long shippingFee(int zone, long orderItemsTotal) {
        if (orderItemsTotal >= config.freeShippingThreshold()) {
            return 0;
        }
        return config.shippingFeeByZone().getOrDefault(zone, config.shippingFeeByZone().get(3));
    }

    public long stampDuty() {
        return config.stampDuty();
    }

    public long codFee() {
        return config.codFee();
    }

    public long codMaxTotal() {
        return config.codMaxTotal();
    }

    public long freeShippingThreshold() {
        return config.freeShippingThreshold();
    }
}
