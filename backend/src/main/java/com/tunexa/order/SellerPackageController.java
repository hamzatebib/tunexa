package com.tunexa.order;

import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.common.PageResponse;
import com.tunexa.config.AuthUser;
import com.tunexa.order.OrderDtos.CancelRequest;
import com.tunexa.order.OrderDtos.SellerPackageDto;
import com.tunexa.order.OrderDtos.ShipRequest;

import jakarta.validation.Valid;

/** Colis à traiter par le vendeur connecté. */
@RestController
@RequestMapping("/api/seller/packages")
public class SellerPackageController {

    private final OrderService orders;

    public SellerPackageController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public PageResponse<SellerPackageDto> list(@AuthenticationPrincipal AuthUser auth,
            @RequestParam(required = false) Set<OrderStatus> status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.sellerPackages(auth.id(), status, page, size);
    }

    @PostMapping("/{id}/accept")
    public SellerPackageDto accept(@AuthenticationPrincipal AuthUser auth, @PathVariable Long id) {
        return orders.accept(auth.id(), id);
    }

    @PostMapping("/{id}/ship")
    public SellerPackageDto ship(@AuthenticationPrincipal AuthUser auth, @PathVariable Long id,
            @Valid @RequestBody ShipRequest req) {
        return orders.ship(auth.id(), id, req.trackingNumber());
    }

    @PostMapping("/{id}/cancel")
    public SellerPackageDto cancel(@AuthenticationPrincipal AuthUser auth, @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelRequest req) {
        return orders.sellerCancel(auth.id(), id, req == null ? null : req.reason());
    }
}
