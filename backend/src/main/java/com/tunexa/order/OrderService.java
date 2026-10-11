package com.tunexa.order;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.auth.OtpPurpose;
import com.tunexa.auth.OtpService;
import com.tunexa.catalog.CatalogService;
import com.tunexa.catalog.OfferRepository;
import com.tunexa.catalog.Product;
import com.tunexa.common.ApiException;
import com.tunexa.common.PageResponse;
import com.tunexa.config.TunexaProperties;
import com.tunexa.order.OrderDtos.EventDto;
import com.tunexa.order.OrderDtos.ItemDto;
import com.tunexa.order.OrderDtos.OrderDetail;
import com.tunexa.order.OrderDtos.OrderSummary;
import com.tunexa.order.OrderDtos.PackageDto;
import com.tunexa.order.OrderDtos.SellerPackageDto;
import com.tunexa.order.OrderDtos.ShippingAddress;
import com.tunexa.seller.Seller;
import com.tunexa.seller.SellerRepository;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final SubOrderRepository subOrders;
    private final OrderEventRepository events;
    private final OfferRepository offers;
    private final SellerRepository sellers;
    private final CatalogService catalog;
    private final OtpService otpService;
    private final TunexaProperties properties;

    public OrderService(OrderRepository orders, SubOrderRepository subOrders, OrderEventRepository events,
            OfferRepository offers, SellerRepository sellers, CatalogService catalog, OtpService otpService,
            TunexaProperties properties) {
        this.orders = orders;
        this.subOrders = subOrders;
        this.events = events;
        this.offers = offers;
        this.sellers = sellers;
        this.catalog = catalog;
        this.otpService = otpService;
        this.properties = properties;
    }

    // ---------------------------------------------------------------- Client

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> list(Long userId, int page, int size) {
        return PageResponse.of(orders.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))), this::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderDetail detail(Long userId, String number) {
        return toDetail(customerOrder(userId, number));
    }

    @Transactional
    public OrderDetail confirm(Long userId, String number, String code) {
        Order order = customerOrder(userId, number);
        if (order.getStatus() != OrderStatus.PENDING_CONFIRMATION) {
            throw ApiException.conflict("not_pending", "Cette commande n'est pas en attente de confirmation");
        }
        otpService.verify(order.getNumber(), OtpPurpose.ORDER_CONFIRMATION, code);
        order.setConfirmedAt(Instant.now());
        for (SubOrder sub : order.getSubOrders()) {
            if (sub.getStatus() == OrderStatus.PENDING_CONFIRMATION) {
                move(sub, OrderStatus.CONFIRMED, null, "CUSTOMER");
            }
        }
        order.refreshStatus();
        return toDetail(order);
    }

    @Transactional
    public String resendConfirmation(Long userId, String number) {
        Order order = customerOrder(userId, number);
        if (order.getStatus() != OrderStatus.PENDING_CONFIRMATION) {
            throw ApiException.conflict("not_pending", "Cette commande n'est pas en attente de confirmation");
        }
        return otpService.send(order.getNumber(), OtpPurpose.ORDER_CONFIRMATION, order.getShipPhone(),
                "Tunexa : code de confirmation de la commande " + order.getNumber() + " :").orElse(null);
    }

    /** Annule tous les colis encore annulables. Refusé si un colis est déjà remis au transporteur. */
    @Transactional
    public OrderDetail cancel(Long userId, String number, String reason) {
        Order order = customerOrder(userId, number);
        List<SubOrder> open = order.getSubOrders().stream().filter(s -> s.getStatus() != OrderStatus.CANCELLED).toList();
        if (open.isEmpty()) {
            throw ApiException.conflict("already_cancelled", "Commande déjà annulée");
        }
        if (open.stream().anyMatch(s -> !s.getStatus().isCancellable())) {
            throw ApiException.conflict("already_shipped",
                    "Un colis a déjà été expédié : refusez-le à la livraison ou demandez un retour");
        }
        String note = reason == null || reason.isBlank() ? "Annulée par le client" : reason.trim();
        open.forEach(sub -> cancelSubOrder(sub, note, "CUSTOMER"));
        order.refreshStatus();
        return toDetail(order);
    }

    /** Annule automatiquement les commandes COD non confirmées dans le délai imparti. */
    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    @Transactional
    public void expireUnconfirmedOrders() {
        Instant limit = Instant.now().minus(properties.orders().confirmationTimeout());
        List<Order> expired = orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_CONFIRMATION, limit);
        for (Order order : expired) {
            order.getSubOrders().stream()
                    .filter(s -> s.getStatus() == OrderStatus.PENDING_CONFIRMATION)
                    .forEach(s -> cancelSubOrder(s, "Non confirmée dans le délai", "SYSTEM"));
            order.refreshStatus();
        }
        if (!expired.isEmpty()) {
            log.info("{} commande(s) non confirmée(s) annulée(s)", expired.size());
        }
    }

    // ---------------------------------------------------------------- Vendeur

    @Transactional(readOnly = true)
    public PageResponse<SellerPackageDto> sellerPackages(Long userId, Set<OrderStatus> statuses, int page, int size) {
        Seller seller = approvedSeller(userId);
        Set<OrderStatus> filter = statuses == null || statuses.isEmpty()
                ? EnumSet.complementOf(EnumSet.of(OrderStatus.PENDING_CONFIRMATION))
                : statuses;
        return PageResponse.of(subOrders.findBySellerIdAndStatusInOrderByIdDesc(seller.getId(), filter,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), this::toSellerPackage);
    }

    @Transactional
    public SellerPackageDto accept(Long userId, Long packageId) {
        SubOrder sub = sellerPackage(userId, packageId);
        requireStatus(sub, OrderStatus.CONFIRMED);
        move(sub, OrderStatus.PROCESSING, "Commande acceptée par le vendeur", "SELLER");
        sub.getOrder().refreshStatus();
        return toSellerPackage(sub);
    }

    @Transactional
    public SellerPackageDto ship(Long userId, Long packageId, String trackingNumber) {
        SubOrder sub = sellerPackage(userId, packageId);
        requireStatus(sub, OrderStatus.PROCESSING);
        sub.setTrackingNumber(trackingNumber.trim());
        move(sub, OrderStatus.SHIPPED, "Remis au transporteur, suivi " + sub.getTrackingNumber(), "SELLER");
        sub.getOrder().refreshStatus();
        return toSellerPackage(sub);
    }

    @Transactional
    public SellerPackageDto sellerCancel(Long userId, Long packageId, String reason) {
        SubOrder sub = sellerPackage(userId, packageId);
        if (sub.getStatus() != OrderStatus.CONFIRMED && sub.getStatus() != OrderStatus.PROCESSING) {
            throw ApiException.conflict("invalid_status", "Ce colis ne peut plus être annulé");
        }
        cancelSubOrder(sub, reason == null || reason.isBlank() ? "Annulée par le vendeur" : reason.trim(), "SELLER");
        sub.getOrder().refreshStatus();
        return toSellerPackage(sub);
    }

    // ---------------------------------------------------------------- Administration / transporteur

    @Transactional(readOnly = true)
    public PageResponse<OrderDetail> allOrders(int page, int size) {
        return PageResponse.of(orders.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), this::toDetail);
    }

    /** Mise à jour de livraison (en attendant l'intégration API des transporteurs). */
    @Transactional
    public OrderDetail updateDelivery(Long packageId, OrderStatus target, String note) {
        SubOrder sub = subOrders.findById(packageId).orElseThrow(() -> ApiException.notFound("Colis"));
        if (!EnumSet.of(OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED, OrderStatus.RETURNED, OrderStatus.SHIPPED)
                .contains(target)) {
            throw ApiException.badRequest("invalid_status", "Statut de livraison invalide");
        }
        requireTransition(sub, target);
        move(sub, target, note, "ADMIN");
        if (target == OrderStatus.RETURNED) {
            restock(sub);
        }
        sub.getOrder().refreshStatus();
        return toDetail(sub.getOrder());
    }

    // ---------------------------------------------------------------- Interne

    void recordEvent(Order order, SubOrder sub, OrderStatus status, String note, String actor) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setSubOrderId(sub == null ? null : sub.getId());
        event.setStatus(status);
        event.setNote(note);
        event.setActor(actor);
        events.save(event);
    }

    private void move(SubOrder sub, OrderStatus target, String note, String actor) {
        requireTransition(sub, target);
        sub.setStatus(target);
        sub.setUpdatedAt(Instant.now());
        recordEvent(sub.getOrder(), sub, target, note, actor);
    }

    private void cancelSubOrder(SubOrder sub, String reason, String actor) {
        sub.setCancelReason(reason);
        move(sub, OrderStatus.CANCELLED, reason, actor);
        restock(sub);
    }

    /** Remet en stock les articles d'un colis annulé ou retourné. */
    private void restock(SubOrder sub) {
        Set<Product> touched = new HashSet<>();
        for (OrderItem item : sub.getItems()) {
            offers.findById(item.getOfferId()).ifPresent(offer -> {
                offer.setStock(offer.getStock() + item.getQuantity());
                touched.add(offer.getProduct());
            });
        }
        touched.forEach(catalog::refreshPricing);
    }

    private void requireStatus(SubOrder sub, OrderStatus expected) {
        if (sub.getStatus() != expected) {
            throw ApiException.conflict("invalid_status", "Action impossible au statut actuel du colis");
        }
    }

    private void requireTransition(SubOrder sub, OrderStatus target) {
        if (!sub.getStatus().canMoveTo(target)) {
            throw ApiException.conflict("invalid_status",
                    "Passage de " + sub.getStatus() + " à " + target + " non autorisé");
        }
    }

    private Order customerOrder(Long userId, String number) {
        return orders.findByNumberAndUserId(number, userId).orElseThrow(() -> ApiException.notFound("Commande"));
    }

    private Seller approvedSeller(Long userId) {
        Seller seller = sellers.findByUserId(userId).orElseThrow(() -> ApiException.forbidden("Compte vendeur requis"));
        if (!seller.isApproved()) {
            throw ApiException.forbidden("Votre compte vendeur n'est pas actif");
        }
        return seller;
    }

    private SubOrder sellerPackage(Long userId, Long packageId) {
        return subOrders.findByIdAndSellerId(packageId, approvedSeller(userId).getId())
                .orElseThrow(() -> ApiException.notFound("Colis"));
    }

    OrderSummary toSummary(Order o) {
        List<OrderItem> items = o.getSubOrders().stream().flatMap(s -> s.getItems().stream()).toList();
        return new OrderSummary(o.getNumber(), o.getStatus(), o.getCreatedAt(), o.getTotal(),
                items.stream().mapToInt(OrderItem::getQuantity).sum(),
                items.isEmpty() ? null : items.get(0).getTitle(), o.getSubOrders().size());
    }

    OrderDetail toDetail(Order o) {
        List<PackageDto> packages = o.getSubOrders().stream()
                .map(s -> new PackageDto(s.getId(), s.getSeller().getShopName(), s.getStatus(), s.getTrackingNumber(),
                        s.getItemsTotal(), s.getShippingFee(), s.getStatus().isCancellable(),
                        s.getItems().stream().map(ItemDto::of).toList()))
                .toList();
        List<EventDto> history = o.getId() == null ? List.of()
                : events.findByOrderIdOrderByCreatedAtAscIdAsc(o.getId()).stream()
                        .map(e -> new EventDto(e.getStatus(), e.getSubOrderId(), e.getNote(), e.getCreatedAt()))
                        .toList();
        boolean cancellable = o.getSubOrders().stream().anyMatch(s -> s.getStatus() != OrderStatus.CANCELLED)
                && o.getSubOrders().stream().filter(s -> s.getStatus() != OrderStatus.CANCELLED)
                        .allMatch(s -> s.getStatus().isCancellable());
        return new OrderDetail(o.getNumber(), o.getStatus(), o.getPaymentMethod(), o.getCreatedAt(),
                o.getConfirmedAt(), o.getItemsTotal(), o.getShippingTotal(), o.getStampDuty(), o.getCodFee(),
                o.getTotal(), cancellable, ShippingAddress.of(o), packages, history);
    }

    private SellerPackageDto toSellerPackage(SubOrder s) {
        Order o = s.getOrder();
        boolean firstPackage = o.getSubOrders().get(0).getId().equals(s.getId());
        // Montant à encaisser par le transporteur pour ce colis ; timbre et frais COD portés par le premier colis
        long toCollect = o.getPaymentMethod() == PaymentMethod.COD
                ? s.getItemsTotal() + s.getShippingFee() + (firstPackage ? o.getStampDuty() + o.getCodFee() : 0)
                : 0;
        return new SellerPackageDto(s.getId(), o.getNumber(), s.getStatus(), o.getCreatedAt(), o.getPaymentMethod(),
                s.getItemsTotal(), s.getShippingFee(), s.getCommission(), s.getItemsTotal() - s.getCommission(),
                toCollect, s.getTrackingNumber(), s.getCancelReason(), ShippingAddress.of(o),
                s.getItems().stream().map(ItemDto::of).toList());
    }
}
