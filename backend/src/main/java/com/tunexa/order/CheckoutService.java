package com.tunexa.order;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.address.Address;
import com.tunexa.address.AddressRepository;
import com.tunexa.auth.OtpPurpose;
import com.tunexa.auth.OtpService;
import com.tunexa.cart.CartItem;
import com.tunexa.cart.CartItemRepository;
import com.tunexa.catalog.CatalogService;
import com.tunexa.catalog.DeliveryEstimator;
import com.tunexa.catalog.Offer;
import com.tunexa.catalog.Product;
import com.tunexa.common.ApiException;
import com.tunexa.common.Money;
import com.tunexa.order.OrderDtos.PlacedOrder;
import com.tunexa.order.OrderDtos.Quote;
import com.tunexa.order.OrderDtos.QuotePackage;
import com.tunexa.seller.Seller;

@Service
public class CheckoutService {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final CartItemRepository cartItems;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final OrderPricing pricing;
    private final DeliveryEstimator deliveryEstimator;
    private final CatalogService catalog;
    private final OtpService otpService;
    private final OrderService orderService;
    private final SecureRandom random = new SecureRandom();

    public CheckoutService(CartItemRepository cartItems, AddressRepository addresses, OrderRepository orders,
            OrderPricing pricing, DeliveryEstimator deliveryEstimator, CatalogService catalog, OtpService otpService,
            OrderService orderService) {
        this.cartItems = cartItems;
        this.addresses = addresses;
        this.orders = orders;
        this.pricing = pricing;
        this.deliveryEstimator = deliveryEstimator;
        this.catalog = catalog;
        this.otpService = otpService;
        this.orderService = orderService;
    }

    /** Articles du panier regroupés par vendeur (un colis par vendeur). */
    private Map<Seller, List<CartItem>> packages(List<CartItem> items) {
        Map<Seller, List<CartItem>> bySeller = new LinkedHashMap<>();
        for (CartItem item : items) {
            bySeller.computeIfAbsent(item.getOffer().getSeller(), k -> new ArrayList<>()).add(item);
        }
        return bySeller;
    }

    @Transactional(readOnly = true)
    public Quote quote(Long userId, Long addressId) {
        Address address = address(userId, addressId);
        List<CartItem> items = cartItems.findByUserIdOrderByAddedAtAsc(userId);
        return quote(items, address.getGovernorate().getShippingZone());
    }

    private Quote quote(List<CartItem> items, int zone) {
        long itemsTotal = items.stream().mapToLong(i -> i.getOffer().getPrice() * i.getQuantity()).sum();
        long vat = items.stream()
                .mapToLong(i -> Money.vatIncluded(i.getOffer().getPrice() * i.getQuantity(),
                        i.getOffer().getProduct().getVatBp()))
                .sum();
        List<QuotePackage> packages = new ArrayList<>();
        packages(items).forEach((seller, lines) -> {
            int handling = lines.stream().mapToInt(l -> l.getOffer().getHandlingDays()).max().orElse(1);
            var estimate = deliveryEstimator.estimate(handling, zone);
            packages.add(new QuotePackage(seller.getId(), seller.getShopName(),
                    lines.stream().mapToLong(l -> l.getOffer().getPrice() * l.getQuantity()).sum(),
                    pricing.shippingFee(zone, itemsTotal), estimate.from(), estimate.to()));
        });
        long shipping = packages.stream().mapToLong(QuotePackage::shippingFee).sum();
        long stamp = items.isEmpty() ? 0 : pricing.stampDuty();
        long codFee = items.isEmpty() ? 0 : pricing.codFee();
        long total = itemsTotal + shipping + stamp + codFee;
        return new Quote(packages, itemsTotal, shipping, stamp, codFee, total, vat, total <= pricing.codMaxTotal(),
                pricing.codMaxTotal(), pricing.freeShippingThreshold());
    }

    @Transactional
    public PlacedOrder placeOrder(Long userId, Long addressId, PaymentMethod paymentMethod) {
        Address address = address(userId, addressId);
        List<CartItem> items = cartItems.findByUserIdOrderByAddedAtAsc(userId);
        if (items.isEmpty()) {
            throw ApiException.badRequest("empty_cart", "Votre panier est vide");
        }
        for (CartItem item : items) {
            Offer offer = item.getOffer();
            if (!offer.isBuyable()) {
                throw ApiException.conflict("offer_unavailable",
                        "« " + offer.getProduct().getTitle() + " » n'est plus disponible");
            }
            if (item.getQuantity() > offer.getStock() || item.getQuantity() > offer.getMaxPerOrder()) {
                throw ApiException.conflict("insufficient_stock",
                        "Stock insuffisant pour « " + offer.getProduct().getTitle() + " »");
            }
        }
        int zone = address.getGovernorate().getShippingZone();
        Quote quote = quote(items, zone);
        if (paymentMethod == PaymentMethod.COD && !quote.codAllowed()) {
            throw ApiException.badRequest("cod_limit_exceeded",
                    "Le paiement à la livraison est limité aux commandes de moins de "
                            + quote.codMaxTotal() / 1000 + " DT");
        }

        Order order = new Order();
        order.setNumber(newOrderNumber());
        order.setUserId(userId);
        order.setPaymentMethod(paymentMethod);
        order.setItemsTotal(quote.itemsTotal());
        order.setShippingTotal(quote.shippingTotal());
        order.setStampDuty(quote.stampDuty());
        order.setCodFee(quote.codFee());
        order.setTotal(quote.total());
        order.setShipFullName(address.getFullName());
        order.setShipPhone(address.getPhone());
        order.setShipGovernorate(address.getGovernorate().getNameFr());
        order.setShipDelegation(address.getDelegation());
        order.setShipLocality(address.getLocality());
        order.setShipPostalCode(address.getPostalCode());
        order.setShipStreet(address.getStreet());
        order.setShipLandmark(address.getLandmark());

        Set<Product> touchedProducts = new HashSet<>();
        packages(items).forEach((seller, lines) -> {
            SubOrder sub = new SubOrder();
            sub.setOrder(order);
            sub.setSeller(seller);
            sub.setStatus(OrderStatus.PENDING_CONFIRMATION);
            sub.setShippingFee(pricing.shippingFee(zone, quote.itemsTotal()));
            for (CartItem line : lines) {
                Offer offer = line.getOffer();
                // Réservation du stock ; le verrou optimiste (@Version) protège des ventes simultanées
                offer.setStock(offer.getStock() - line.getQuantity());
                touchedProducts.add(offer.getProduct());

                OrderItem item = new OrderItem();
                item.setSubOrder(sub);
                item.setOfferId(offer.getId());
                item.setProductId(offer.getProduct().getId());
                item.setTitle(offer.getProduct().getTitle());
                item.setUnitPrice(offer.getPrice());
                item.setQuantity(line.getQuantity());
                item.setVatBp(offer.getProduct().getVatBp());
                item.setLineTotal(offer.getPrice() * line.getQuantity());
                sub.getItems().add(item);
            }
            sub.setItemsTotal(sub.getItems().stream().mapToLong(OrderItem::getLineTotal).sum());
            sub.setCommission(Money.percentOf(sub.getItemsTotal(), seller.getCommissionBp()));
            order.getSubOrders().add(sub);
        });
        order.refreshStatus();
        orders.save(order);
        orderService.recordEvent(order, null, OrderStatus.PENDING_CONFIRMATION, "Commande passée", "CUSTOMER");

        cartItems.deleteAll(items);
        touchedProducts.forEach(catalog::refreshPricing);

        String devCode = otpService.send(order.getNumber(), OtpPurpose.ORDER_CONFIRMATION, order.getShipPhone(),
                "Tunexa : commande " + order.getNumber() + " de " + formatTnd(order.getTotal())
                        + ". Code de confirmation :").orElse(null);
        return new PlacedOrder(orderService.toDetail(order), devCode);
    }

    private Address address(Long userId, Long addressId) {
        return addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> ApiException.badRequest("invalid_address", "Adresse de livraison introuvable"));
    }

    private String newOrderNumber() {
        String prefix = "TX" + LocalDate.now(TUNIS).format(NUMBER_DATE);
        String number;
        do {
            number = prefix + String.format("%06d", random.nextInt(1_000_000));
        } while (orders.existsByNumber(number));
        return number;
    }

    static String formatTnd(long millimes) {
        return String.format("%d,%03d DT", millimes / 1000, millimes % 1000);
    }
}
