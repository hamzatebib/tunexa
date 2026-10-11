package com.tunexa.cart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.catalog.Offer;
import com.tunexa.catalog.OfferRepository;
import com.tunexa.common.ApiException;

@Service
public class CartService {

    public record CartLine(Long offerId, Long productId, String title, String brand, String imageUrl, Long sellerId,
            String shopName, long unitPrice, int quantity, int maxQuantity, long lineTotal, boolean available) {
    }

    public record SellerGroup(Long sellerId, String shopName, List<CartLine> lines, long subtotal) {
    }

    public record CartView(List<SellerGroup> groups, int itemCount, long itemsTotal, boolean checkoutAllowed) {
    }

    private final CartItemRepository items;
    private final OfferRepository offers;

    public CartService(CartItemRepository items, OfferRepository offers) {
        this.items = items;
        this.offers = offers;
    }

    @Transactional(readOnly = true)
    public CartView view(Long userId) {
        Map<Long, List<CartLine>> bySeller = new LinkedHashMap<>();
        for (CartItem item : items.findByUserIdOrderByAddedAtAsc(userId)) {
            Offer o = item.getOffer();
            boolean available = o.isBuyable();
            int max = available ? Math.min(o.getStock(), o.getMaxPerOrder()) : 0;
            CartLine line = new CartLine(o.getId(), o.getProduct().getId(), o.getProduct().getTitle(),
                    o.getProduct().getBrand() == null ? null : o.getProduct().getBrand().getName(),
                    o.getProduct().getImageUrl(), o.getSeller().getId(), o.getSeller().getShopName(), o.getPrice(),
                    item.getQuantity(), max, o.getPrice() * item.getQuantity(), available && item.getQuantity() <= max);
            bySeller.computeIfAbsent(o.getSeller().getId(), k -> new ArrayList<>()).add(line);
        }
        List<SellerGroup> groups = bySeller.values().stream()
                .map(lines -> new SellerGroup(lines.get(0).sellerId(), lines.get(0).shopName(), lines,
                        lines.stream().filter(CartLine::available).mapToLong(CartLine::lineTotal).sum()))
                .toList();
        List<CartLine> all = groups.stream().flatMap(g -> g.lines().stream()).toList();
        long total = groups.stream().mapToLong(SellerGroup::subtotal).sum();
        int count = all.stream().filter(CartLine::available).mapToInt(CartLine::quantity).sum();
        boolean allowed = !all.isEmpty() && all.stream().allMatch(CartLine::available);
        return new CartView(groups, count, total, allowed);
    }

    @Transactional
    public CartView add(Long userId, Long offerId, int quantity) {
        Offer offer = offers.findById(offerId).filter(Offer::isBuyable)
                .orElseThrow(() -> ApiException.badRequest("offer_unavailable", "Cette offre n'est plus disponible"));
        CartItem item = items.findByUserIdAndOfferId(userId, offerId).orElseGet(() -> {
            CartItem created = new CartItem();
            created.setUserId(userId);
            created.setOffer(offer);
            return created;
        });
        item.setQuantity(checkQuantity(offer, item.getQuantity() + quantity));
        items.save(item);
        return view(userId);
    }

    @Transactional
    public CartView update(Long userId, Long offerId, int quantity) {
        CartItem item = items.findByUserIdAndOfferId(userId, offerId)
                .orElseThrow(() -> ApiException.notFound("Article"));
        if (quantity <= 0) {
            items.delete(item);
        } else {
            item.setQuantity(checkQuantity(item.getOffer(), quantity));
        }
        return view(userId);
    }

    @Transactional
    public CartView remove(Long userId, Long offerId) {
        items.findByUserIdAndOfferId(userId, offerId).ifPresent(items::delete);
        return view(userId);
    }

    private int checkQuantity(Offer offer, int quantity) {
        if (quantity < 1) {
            throw ApiException.badRequest("invalid_quantity", "Quantité invalide");
        }
        if (quantity > offer.getMaxPerOrder()) {
            throw ApiException.badRequest("max_per_order",
                    "Quantité maximale par commande : " + offer.getMaxPerOrder());
        }
        if (quantity > offer.getStock()) {
            throw ApiException.badRequest("insufficient_stock", "Stock disponible : " + offer.getStock());
        }
        return quantity;
    }
}
