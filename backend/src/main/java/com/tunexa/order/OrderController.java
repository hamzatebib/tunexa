package com.tunexa.order;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.common.PageResponse;
import com.tunexa.config.AuthUser;
import com.tunexa.order.OrderDtos.CancelRequest;
import com.tunexa.order.OrderDtos.CheckoutRequest;
import com.tunexa.order.OrderDtos.ConfirmRequest;
import com.tunexa.order.OrderDtos.OrderDetail;
import com.tunexa.order.OrderDtos.OrderSummary;
import com.tunexa.order.OrderDtos.PlacedOrder;
import com.tunexa.order.OrderDtos.Quote;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final CheckoutService checkout;
    private final OrderService orders;

    public OrderController(CheckoutService checkout, OrderService orders) {
        this.checkout = checkout;
        this.orders = orders;
    }

    @GetMapping("/checkout/quote")
    public Quote quote(@AuthenticationPrincipal AuthUser auth, @RequestParam Long addressId) {
        return checkout.quote(auth.id(), addressId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacedOrder placeOrder(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody CheckoutRequest req) {
        return checkout.placeOrder(auth.id(), req.addressId(), req.paymentMethod());
    }

    @GetMapping("/orders")
    public PageResponse<OrderSummary> list(@AuthenticationPrincipal AuthUser auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return orders.list(auth.id(), page, size);
    }

    @GetMapping("/orders/{number}")
    public OrderDetail detail(@AuthenticationPrincipal AuthUser auth, @PathVariable String number) {
        return orders.detail(auth.id(), number);
    }

    @PostMapping("/orders/{number}/confirm")
    public OrderDetail confirm(@AuthenticationPrincipal AuthUser auth, @PathVariable String number,
            @Valid @RequestBody ConfirmRequest req) {
        return orders.confirm(auth.id(), number, req.code());
    }

    @PostMapping("/orders/{number}/resend-confirmation")
    public Map<String, Object> resend(@AuthenticationPrincipal AuthUser auth, @PathVariable String number) {
        String devCode = orders.resendConfirmation(auth.id(), number);
        return devCode == null ? Map.of("sent", true) : Map.of("sent", true, "devCode", devCode);
    }

    @PostMapping("/orders/{number}/cancel")
    public OrderDetail cancel(@AuthenticationPrincipal AuthUser auth, @PathVariable String number,
            @Valid @RequestBody(required = false) CancelRequest req) {
        return orders.cancel(auth.id(), number, req == null ? null : req.reason());
    }
}
