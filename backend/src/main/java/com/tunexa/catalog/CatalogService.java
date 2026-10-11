package com.tunexa.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.catalog.CatalogDtos.BrandDto;
import com.tunexa.catalog.CatalogDtos.CategoryNode;
import com.tunexa.catalog.CatalogDtos.CategoryRef;
import com.tunexa.catalog.CatalogDtos.OfferDto;
import com.tunexa.catalog.CatalogDtos.ProductCard;
import com.tunexa.catalog.CatalogDtos.ProductDetail;
import com.tunexa.catalog.CatalogDtos.SearchParams;
import com.tunexa.common.ApiException;
import com.tunexa.common.PageResponse;
import com.tunexa.geo.Governorate;
import com.tunexa.geo.GovernorateRepository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
public class CatalogService {

    /**
     * Ordre de la Buy Box : offre achetable la moins chère, puis délai de préparation le plus court,
     * puis l'offre la plus ancienne.
     */
    static final Comparator<Offer> BUY_BOX_ORDER = Comparator.comparingLong(Offer::getPrice)
            .thenComparingInt(Offer::getHandlingDays)
            .thenComparing(Offer::getId);

    private static final int DEFAULT_ZONE = 1;

    private final ProductRepository products;
    private final OfferRepository offers;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final GovernorateRepository governorates;
    private final DeliveryEstimator deliveryEstimator;

    public CatalogService(ProductRepository products, OfferRepository offers, CategoryRepository categories,
            BrandRepository brands, GovernorateRepository governorates, DeliveryEstimator deliveryEstimator) {
        this.products = products;
        this.offers = offers;
        this.categories = categories;
        this.brands = brands;
        this.governorates = governorates;
        this.deliveryEstimator = deliveryEstimator;
    }

    @Transactional(readOnly = true)
    public List<CategoryNode> categoryTree() {
        List<Category> all = categories.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, List<Category>> byParent = all.stream().filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));
        return all.stream().filter(c -> c.getParentId() == null).map(c -> toNode(c, byParent)).toList();
    }

    private CategoryNode toNode(Category c, Map<Long, List<Category>> byParent) {
        List<CategoryNode> children = byParent.getOrDefault(c.getId(), List.of()).stream()
                .map(child -> toNode(child, byParent)).toList();
        return new CategoryNode(c.getId(), c.getSlug(), c.getNameFr(), c.getNameAr(), c.getNameEn(), children);
    }

    @Transactional(readOnly = true)
    public List<BrandDto> brands() {
        return brands.findAllByOrderByNameAsc().stream().map(BrandDto::of).toList();
    }

    Set<Long> categoryWithDescendants(Long categoryId) {
        List<Category> all = categories.findAll();
        Set<Long> result = new HashSet<>();
        result.add(categoryId);
        boolean added = true;
        while (added) {
            added = false;
            for (Category c : all) {
                if (c.getParentId() != null && result.contains(c.getParentId()) && result.add(c.getId())) {
                    added = true;
                }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductCard> search(SearchParams params, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.PUBLISHED));
            predicates.add(cb.isNotNull(root.get("bestPrice")));
            if (params.q() != null && !params.q().isBlank()) {
                var brand = root.join("brand", JoinType.LEFT);
                for (String token : params.q().trim().toLowerCase(Locale.ROOT).split("\\s+")) {
                    String like = "%" + token.replace("%", "").replace("_", "") + "%";
                    predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                            cb.like(cb.lower(brand.get("name")), like)));
                }
            }
            if (params.categoryId() != null) {
                predicates.add(root.get("category").get("id").in(categoryWithDescendants(params.categoryId())));
            }
            if (params.brandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), params.brandId()));
            }
            if (params.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bestPrice"), params.minPrice()));
            }
            if (params.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bestPrice"), params.maxPrice()));
            }
            if (Boolean.TRUE.equals(params.inStock())) {
                predicates.add(cb.isTrue(root.get("inStock")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Sort sort = switch (params.sort() == null ? "" : params.sort()) {
            case "price_asc" -> Sort.by(Sort.Order.asc("bestPrice"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("bestPrice"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("inStock"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
        int safeSize = Math.min(Math.max(size, 1), 60);
        return PageResponse.of(products.findAll(spec, PageRequest.of(Math.max(page, 0), safeSize, sort)),
                ProductCard::of);
    }

    @Transactional(readOnly = true)
    public ProductDetail detail(Long productId, Long governorateId) {
        Product product = products.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Produit"));
        int zone = Optional.ofNullable(governorateId).flatMap(governorates::findById)
                .map(Governorate::getShippingZone).orElse(DEFAULT_ZONE);

        List<Offer> visible = offers.findByProductId(productId).stream()
                .filter(o -> o.isActive() && o.getSeller().isApproved())
                .toList();
        List<Offer> buyable = visible.stream().filter(o -> o.getStock() > 0).sorted(BUY_BOX_ORDER).toList();
        Offer buyBox = buyable.isEmpty() ? null : buyable.get(0);
        List<OfferDto> others = visible.stream()
                .filter(o -> o != buyBox)
                .sorted(Comparator.comparing((Offer o) -> o.getStock() <= 0).thenComparing(BUY_BOX_ORDER))
                .map(o -> toOfferDto(o, zone))
                .toList();

        return new ProductDetail(product.getId(), product.getTitle(), product.getDescription(),
                BrandDto.of(product.getBrand()), breadcrumb(product.getCategory()), product.getImageUrl(),
                product.getEan(), product.getVatBp(), buyBox == null ? null : toOfferDto(buyBox, zone), others);
    }

    private List<CategoryRef> breadcrumb(Category category) {
        Map<Long, Category> all = categories.findAll().stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        List<CategoryRef> path = new ArrayList<>();
        Category current = all.get(category.getId());
        while (current != null) {
            path.add(CategoryRef.of(current));
            current = current.getParentId() == null ? null : all.get(current.getParentId());
        }
        Collections.reverse(path);
        return path;
    }

    private OfferDto toOfferDto(Offer o, int zone) {
        var estimate = deliveryEstimator.estimate(o.getHandlingDays(), zone);
        return new OfferDto(o.getId(), o.getSeller().getId(), o.getSeller().getShopName(), o.getSeller().getRating(),
                o.getPrice(), o.getCompareAtPrice(), o.getStock() > 0, Math.min(o.getStock(), o.getMaxPerOrder()),
                o.getHandlingDays(), o.getCondition(), estimate.from(), estimate.to());
    }

    /** Recalcule le prix affiché et la disponibilité du produit après tout changement d'offre. */
    @Transactional
    public void refreshPricing(Product product) {
        List<Offer> visible = offers.findByProductId(product.getId()).stream()
                .filter(o -> o.isActive() && o.getSeller().isApproved())
                .toList();
        Optional<Offer> best = visible.stream().filter(o -> o.getStock() > 0).min(BUY_BOX_ORDER);
        product.setInStock(best.isPresent());
        product.setBestPrice(best.map(Offer::getPrice)
                .orElseGet(() -> visible.stream().map(Offer::getPrice).min(Long::compare).orElse(null)));
    }
}
