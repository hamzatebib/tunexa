package com.tunexa.seller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.config.AuthUser;
import com.tunexa.seller.SellerDtos.ApplyRequest;
import com.tunexa.seller.SellerDtos.AttachOfferRequest;
import com.tunexa.seller.SellerDtos.CreateProductRequest;
import com.tunexa.seller.SellerDtos.SellerDto;
import com.tunexa.seller.SellerDtos.SellerOfferDto;
import com.tunexa.seller.SellerDtos.UpdateOfferRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public SellerDto apply(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody ApplyRequest req) {
        return sellerService.apply(auth.id(), req);
    }

    @GetMapping("/me")
    public SellerDto me(@AuthenticationPrincipal AuthUser auth) {
        return sellerService.me(auth.id());
    }

    @GetMapping("/offers")
    public List<SellerOfferDto> offers(@AuthenticationPrincipal AuthUser auth) {
        return sellerService.offers(auth.id());
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public SellerOfferDto createProduct(@AuthenticationPrincipal AuthUser auth,
            @Valid @RequestBody CreateProductRequest req) {
        return sellerService.createProduct(auth.id(), req);
    }

    @PostMapping("/offers")
    @ResponseStatus(HttpStatus.CREATED)
    public SellerOfferDto attachOffer(@AuthenticationPrincipal AuthUser auth,
            @Valid @RequestBody AttachOfferRequest req) {
        return sellerService.attachOffer(auth.id(), req.productId(), req.offer());
    }

    @PutMapping("/offers/{id}")
    public SellerOfferDto updateOffer(@AuthenticationPrincipal AuthUser auth, @PathVariable Long id,
            @Valid @RequestBody UpdateOfferRequest req) {
        return sellerService.updateOffer(auth.id(), id, req.offer(), req.active());
    }
}
