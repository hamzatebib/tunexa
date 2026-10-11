package com.tunexa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tunexa.auth.PhoneNumbers;
import com.tunexa.common.ApiException;
import com.tunexa.common.Money;
import com.tunexa.order.Order;
import com.tunexa.order.OrderStatus;
import com.tunexa.order.SubOrder;

class DomainRulesTest {

    @Test
    void normalizesTunisianMobileNumbers() {
        assertThat(PhoneNumbers.normalize("22 123 456")).isEqualTo("+21622123456");
        assertThat(PhoneNumbers.normalize("0021622123456")).isEqualTo("+21622123456");
        assertThat(PhoneNumbers.normalize("216-22-123-456")).isEqualTo("+21622123456");
        assertThatThrownBy(() -> PhoneNumbers.normalize("+21612345678")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize("+33612345678")).isInstanceOf(ApiException.class);
    }

    @Test
    void computesCommissionAndIncludedVat() {
        assertThat(Money.percentOf(1_598_000, 1000)).isEqualTo(159_800);
        assertThat(Money.vatIncluded(119_000, 1900)).isEqualTo(19_000);
        assertThat(Money.vatIncluded(107_000, 700)).isEqualTo(7_000);
    }

    @Test
    void enforcesPackageTransitions() {
        assertThat(OrderStatus.CONFIRMED.canMoveTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canMoveTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.SHIPPED.isCancellable()).isFalse();
        assertThat(OrderStatus.DELIVERED.canMoveTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void derivesOrderStatusFromPackages() {
        assertThat(statusOf(OrderStatus.CONFIRMED, OrderStatus.PROCESSING)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(statusOf(OrderStatus.SHIPPED, OrderStatus.PROCESSING)).isEqualTo(OrderStatus.SHIPPED);
        assertThat(statusOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)).isEqualTo(OrderStatus.DELIVERED);
        assertThat(statusOf(OrderStatus.CANCELLED, OrderStatus.CANCELLED)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(statusOf(OrderStatus.PENDING_CONFIRMATION, OrderStatus.PENDING_CONFIRMATION))
                .isEqualTo(OrderStatus.PENDING_CONFIRMATION);
    }

    @Test
    void skipsSundaysWhenEstimatingDelivery() throws Exception {
        var method = com.tunexa.catalog.DeliveryEstimator.class.getDeclaredMethod("addWorkingDays", LocalDate.class,
                int.class);
        method.setAccessible(true);
        // Samedi 10 octobre 2026 + 1 jour ouvré = lundi 12 octobre
        assertThat(method.invoke(null, LocalDate.of(2026, 10, 10), 1)).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    private OrderStatus statusOf(OrderStatus... statuses) {
        Order order = new Order();
        order.setSubOrders(new java.util.ArrayList<>(List.of(statuses).stream().map(s -> {
            SubOrder sub = new SubOrder();
            sub.setStatus(s);
            return sub;
        }).toList()));
        order.refreshStatus();
        return order.getStatus();
    }
}
