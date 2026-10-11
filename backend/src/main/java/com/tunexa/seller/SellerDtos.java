package com.tunexa.seller;

import java.time.Instant;

import com.tunexa.catalog.Offer;
import com.tunexa.catalog.ProductStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class SellerDtos {

    private SellerDtos() {
    }

    public record ApplyRequest(
            @NotBlank @Size(max = 120) String shopName,
            @NotBlank @Size(max = 160) String legalName,
            @NotBlank String taxId,
            @NotBlank @Size(max = 30) String rneNumber,
            @NotBlank @Pattern(regexp = "\\d{20}", message = "Le RIB contient 20 chiffres") String rib,
            @NotBlank @Size(max = 250) String pickupAddress,
            @NotNull Long governorateId) {
    }

    public record SellerDto(Long id, String shopName, String legalName, String taxId, String rneNumber,
            String governorateName, SellerStatus status, String rejectionReason, int commissionBp, Instant createdAt) {

        public static SellerDto of(Seller s) {
            return new SellerDto(s.getId(), s.getShopName(), s.getLegalName(), s.getTaxId(), s.getRneNumber(),
                    s.getGovernorate().getNameFr(), s.getStatus(), s.getRejectionReason(), s.getCommissionBp(),
                    s.getCreatedAt());
        }
    }

    /** Champs commerciaux d'une offre. Prix TTC en millimes. */
    public record OfferFields(
            @NotNull @Positive Long price,
            @Positive Long compareAtPrice,
            @NotNull @Min(0) @Max(100_000) Integer stock,
            @NotNull @Min(0) @Max(30) Integer handlingDays) {
    }

    public record CreateProductRequest(
            @NotNull Long categoryId,
            @Size(max = 80) String brandName,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            @Pattern(regexp = "\\d{8,14}", message = "EAN de 8 à 14 chiffres") String ean,
            @Size(max = 500) String imageUrl,
            @NotNull @jakarta.validation.Valid OfferFields offer) {
    }

    public record AttachOfferRequest(@NotNull Long productId, @NotNull @jakarta.validation.Valid OfferFields offer) {
    }

    public record UpdateOfferRequest(@NotNull @jakarta.validation.Valid OfferFields offer, boolean active) {
    }

    public record SellerOfferDto(Long offerId, Long productId, String title, String brand, String imageUrl,
            ProductStatus productStatus, String rejectionReason, long price, Long compareAtPrice, int stock,
            int handlingDays, boolean active) {

        public static SellerOfferDto of(Offer o) {
            var p = o.getProduct();
            return new SellerOfferDto(o.getId(), p.getId(), p.getTitle(),
                    p.getBrand() == null ? null : p.getBrand().getName(), p.getImageUrl(), p.getStatus(),
                    p.getRejectionReason(), o.getPrice(), o.getCompareAtPrice(), o.getStock(), o.getHandlingDays(),
                    o.isActive());
        }
    }
}
