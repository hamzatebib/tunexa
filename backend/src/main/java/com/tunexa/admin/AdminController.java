package com.tunexa.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.catalog.CatalogService;
import com.tunexa.catalog.Product;
import com.tunexa.catalog.ProductRepository;
import com.tunexa.catalog.ProductStatus;
import com.tunexa.common.ApiException;
import com.tunexa.common.PageResponse;
import com.tunexa.order.OrderDtos.OrderDetail;
import com.tunexa.order.OrderRepository;
import com.tunexa.order.OrderService;
import com.tunexa.order.OrderStatus;
import com.tunexa.order.SubOrderRepository;
import com.tunexa.seller.Seller;
import com.tunexa.seller.SellerDtos.SellerDto;
import com.tunexa.seller.SellerRepository;
import com.tunexa.seller.SellerStatus;
import com.tunexa.user.Role;
import com.tunexa.user.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Back-office : validation des vendeurs, modération du catalogue, suivi des commandes. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record Stats(long ordersToday, long revenueToday, long ordersLast30Days, long revenueLast30Days,
            long pendingSellers, long pendingProducts, long packagesToShip, long customers) {
    }

    public record RejectRequest(@NotBlank @Size(max = 300) String reason) {
    }

    public record DeliveryUpdate(@NotNull OrderStatus status, @Size(max = 300) String note) {
    }

    public record PendingProduct(Long id, String title, String brand, String category, String imageUrl,
            String description, String ean, Long price, String sellerShopName, Instant createdAt) {
    }

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final SellerRepository sellers;
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final SubOrderRepository subOrders;
    private final OrderService orderService;
    private final CatalogService catalog;

    public AdminController(SellerRepository sellers, UserRepository users, ProductRepository products,
            OrderRepository orders, SubOrderRepository subOrders, OrderService orderService, CatalogService catalog) {
        this.sellers = sellers;
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.subOrders = subOrders;
        this.orderService = orderService;
        this.catalog = catalog;
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Stats stats() {
        Instant startOfDay = LocalDate.now(TUNIS).atStartOfDay(TUNIS).toInstant();
        Instant thirtyDays = startOfDay.minusSeconds(30L * 24 * 3600);
        return new Stats(orders.countByCreatedAtAfter(startOfDay), orders.sumTotalSince(startOfDay),
                orders.countByCreatedAtAfter(thirtyDays), orders.sumTotalSince(thirtyDays),
                sellers.countByStatus(SellerStatus.PENDING), products.countByStatus(ProductStatus.PENDING_REVIEW),
                subOrders.countByStatus(OrderStatus.CONFIRMED) + subOrders.countByStatus(OrderStatus.PROCESSING),
                users.count());
    }

    // ---------------------------------------------------------------- Vendeurs

    @GetMapping("/sellers")
    @Transactional(readOnly = true)
    public List<SellerDto> sellers(@RequestParam(defaultValue = "PENDING") SellerStatus status) {
        return sellers.findByStatusOrderByCreatedAtAsc(status).stream().map(SellerDto::of).toList();
    }

    @PostMapping("/sellers/{id}/approve")
    @Transactional
    public SellerDto approveSeller(@PathVariable Long id) {
        Seller seller = sellers.findById(id).orElseThrow(() -> ApiException.notFound("Vendeur"));
        seller.setStatus(SellerStatus.APPROVED);
        seller.setRejectionReason(null);
        users.findById(seller.getUserId()).filter(u -> u.getRole() == Role.CUSTOMER)
                .ifPresent(u -> u.setRole(Role.SELLER));
        return SellerDto.of(seller);
    }

    @PostMapping("/sellers/{id}/reject")
    @Transactional
    public SellerDto rejectSeller(@PathVariable Long id, @Valid @RequestBody RejectRequest req) {
        Seller seller = sellers.findById(id).orElseThrow(() -> ApiException.notFound("Vendeur"));
        if (seller.getStatus() != SellerStatus.PENDING) {
            throw ApiException.conflict("invalid_status", "Seules les demandes en attente peuvent être refusées");
        }
        seller.setStatus(SellerStatus.REJECTED);
        seller.setRejectionReason(req.reason().trim());
        return SellerDto.of(seller);
    }

    // ---------------------------------------------------------------- Modération des produits

    @GetMapping("/products/pending")
    @Transactional(readOnly = true)
    public List<PendingProduct> pendingProducts() {
        return products.findAll((root, q, cb) -> cb.equal(root.get("status"), ProductStatus.PENDING_REVIEW),
                org.springframework.data.domain.PageRequest.of(0, 100,
                        org.springframework.data.domain.Sort.by("createdAt")))
                .map(p -> new PendingProduct(p.getId(), p.getTitle(),
                        p.getBrand() == null ? null : p.getBrand().getName(), p.getCategory().getNameFr(),
                        p.getImageUrl(), p.getDescription(), p.getEan(), p.getBestPrice(),
                        p.getCreatedBySellerId() == null ? null
                                : sellers.findById(p.getCreatedBySellerId()).map(Seller::getShopName).orElse(null),
                        p.getCreatedAt()))
                .getContent();
    }

    @PostMapping("/products/{id}/approve")
    @Transactional
    public void approveProduct(@PathVariable Long id) {
        Product product = products.findById(id).orElseThrow(() -> ApiException.notFound("Produit"));
        product.setStatus(ProductStatus.PUBLISHED);
        product.setRejectionReason(null);
        catalog.refreshPricing(product);
    }

    @PostMapping("/products/{id}/reject")
    @Transactional
    public void rejectProduct(@PathVariable Long id, @Valid @RequestBody RejectRequest req) {
        Product product = products.findById(id).orElseThrow(() -> ApiException.notFound("Produit"));
        product.setStatus(ProductStatus.REJECTED);
        product.setRejectionReason(req.reason().trim());
    }

    // ---------------------------------------------------------------- Commandes

    @GetMapping("/orders")
    public PageResponse<OrderDetail> orders(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.allOrders(page, size);
    }

    @PostMapping("/packages/{id}/delivery")
    public OrderDetail updateDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryUpdate req) {
        return orderService.updateDelivery(id, req.status(), req.note());
    }
}
