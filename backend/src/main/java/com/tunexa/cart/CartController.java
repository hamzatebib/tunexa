package com.tunexa.cart;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.cart.CartService.CartView;
import com.tunexa.config.AuthUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    public record AddRequest(@NotNull Long offerId, @Min(1) @Max(100) int quantity) {
    }

    public record QuantityRequest(@Min(0) @Max(100) int quantity) {
    }

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    public CartView view(@AuthenticationPrincipal AuthUser auth) {
        return cart.view(auth.id());
    }

    @PostMapping("/items")
    public CartView add(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody AddRequest req) {
        return cart.add(auth.id(), req.offerId(), req.quantity());
    }

    @PutMapping("/items/{offerId}")
    public CartView update(@AuthenticationPrincipal AuthUser auth, @PathVariable Long offerId,
            @Valid @RequestBody QuantityRequest req) {
        return cart.update(auth.id(), offerId, req.quantity());
    }

    @DeleteMapping("/items/{offerId}")
    public CartView remove(@AuthenticationPrincipal AuthUser auth, @PathVariable Long offerId) {
        return cart.remove(auth.id(), offerId);
    }
}
