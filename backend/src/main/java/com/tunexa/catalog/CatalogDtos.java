package com.tunexa.catalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryNode(Long id, String slug, String nameFr, String nameAr, String nameEn,
            List<CategoryNode> children) {
    }

    public record CategoryRef(Long id, String slug, String nameFr, String nameAr, String nameEn) {

        static CategoryRef of(Category c) {
            return new CategoryRef(c.getId(), c.getSlug(), c.getNameFr(), c.getNameAr(), c.getNameEn());
        }
    }

    public record BrandDto(Long id, String name) {

        static BrandDto of(Brand b) {
            return b == null ? null : new BrandDto(b.getId(), b.getName());
        }
    }

    public record ProductCard(Long id, String title, String brand, String imageUrl, Long price, boolean inStock,
            Long categoryId) {

        static ProductCard of(Product p) {
            return new ProductCard(p.getId(), p.getTitle(), p.getBrand() == null ? null : p.getBrand().getName(),
                    p.getImageUrl(), p.getBestPrice(), p.isInStock(), p.getCategory().getId());
        }
    }

    public record OfferDto(Long id, Long sellerId, String shopName, BigDecimal sellerRating, long price,
            Long compareAtPrice, boolean inStock, int maxQuantity, int handlingDays, String condition,
            LocalDate deliveryFrom, LocalDate deliveryTo) {
    }

    public record ProductDetail(Long id, String title, String description, BrandDto brand,
            List<CategoryRef> breadcrumb, String imageUrl, String ean, int vatBp, OfferDto buyBox,
            List<OfferDto> otherOffers) {
    }

    public record SearchParams(String q, Long categoryId, Long brandId, Long minPrice, Long maxPrice,
            Boolean inStock, String sort) {
    }
}
