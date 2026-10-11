package com.tunexa.order;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CheckoutRequest(@NotNull Long addressId, @NotNull PaymentMethod paymentMethod) {
    }

    public record QuotePackage(Long sellerId, String shopName, long itemsTotal, long shippingFee,
            LocalDate deliveryFrom, LocalDate deliveryTo) {
    }

    public record Quote(List<QuotePackage> packages, long itemsTotal, long shippingTotal, long stampDuty, long codFee,
            long total, long vatIncluded, boolean codAllowed, long codMaxTotal, long freeShippingThreshold) {
    }

    public record ShippingAddress(String fullName, String phone, String governorate, String delegation,
            String locality, String postalCode, String street, String landmark) {

        static ShippingAddress of(Order o) {
            return new ShippingAddress(o.getShipFullName(), o.getShipPhone(), o.getShipGovernorate(),
                    o.getShipDelegation(), o.getShipLocality(), o.getShipPostalCode(), o.getShipStreet(),
                    o.getShipLandmark());
        }
    }

    public record ItemDto(Long productId, Long offerId, String title, long unitPrice, int quantity, long lineTotal) {

        static ItemDto of(OrderItem i) {
            return new ItemDto(i.getProductId(), i.getOfferId(), i.getTitle(), i.getUnitPrice(), i.getQuantity(),
                    i.getLineTotal());
        }
    }

    public record PackageDto(Long id, String shopName, OrderStatus status, String trackingNumber, long itemsTotal,
            long shippingFee, boolean cancellable, List<ItemDto> items) {
    }

    public record EventDto(OrderStatus status, Long packageId, String note, Instant createdAt) {
    }

    public record OrderDetail(String number, OrderStatus status, PaymentMethod paymentMethod, Instant createdAt,
            Instant confirmedAt, long itemsTotal, long shippingTotal, long stampDuty, long codFee, long total,
            boolean cancellable, ShippingAddress shippingAddress, List<PackageDto> packages, List<EventDto> events) {
    }

    /** {@code devConfirmationCode} n'est renseigné qu'en développement. */
    public record PlacedOrder(OrderDetail order, String devConfirmationCode) {
    }

    public record OrderSummary(String number, OrderStatus status, Instant createdAt, long total, int itemCount,
            String firstItemTitle, int packageCount) {
    }

    public record ConfirmRequest(@NotBlank @Size(min = 6, max = 6) String code) {
    }

    public record CancelRequest(@Size(max = 300) String reason) {
    }

    public record ShipRequest(@NotBlank @Size(max = 40) String trackingNumber) {
    }

    public record SellerPackageDto(Long id, String orderNumber, OrderStatus status, Instant orderedAt,
            PaymentMethod paymentMethod, long itemsTotal, long shippingFee, long commission, long netAmount,
            long amountToCollect, String trackingNumber, String cancelReason, ShippingAddress shipTo,
            List<ItemDto> items) {
    }
}
