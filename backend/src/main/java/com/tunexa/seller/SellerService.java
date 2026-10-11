package com.tunexa.seller;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.catalog.Brand;
import com.tunexa.catalog.BrandRepository;
import com.tunexa.catalog.CatalogService;
import com.tunexa.catalog.Category;
import com.tunexa.catalog.CategoryRepository;
import com.tunexa.catalog.Offer;
import com.tunexa.catalog.OfferRepository;
import com.tunexa.catalog.Product;
import com.tunexa.catalog.ProductRepository;
import com.tunexa.catalog.ProductStatus;
import com.tunexa.common.ApiException;
import com.tunexa.config.TunexaProperties;
import com.tunexa.geo.GovernorateRepository;
import com.tunexa.seller.SellerDtos.ApplyRequest;
import com.tunexa.seller.SellerDtos.CreateProductRequest;
import com.tunexa.seller.SellerDtos.OfferFields;
import com.tunexa.seller.SellerDtos.SellerDto;
import com.tunexa.seller.SellerDtos.SellerOfferDto;

@Service
public class SellerService {

    /** Matricule fiscal tunisien : 7 chiffres, clé, code TVA, catégorie, établissement (ex. 1234567A/A/M/000). */
    private static final java.util.regex.Pattern TAX_ID =
            java.util.regex.Pattern.compile("^\\d{7}[A-Z]/?[A-Z]/?[A-Z]/?\\d{3}$");

    private final SellerRepository sellers;
    private final GovernorateRepository governorates;
    private final ProductRepository products;
    private final OfferRepository offers;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final CatalogService catalog;
    private final TunexaProperties properties;

    public SellerService(SellerRepository sellers, GovernorateRepository governorates, ProductRepository products,
            OfferRepository offers, CategoryRepository categories, BrandRepository brands, CatalogService catalog,
            TunexaProperties properties) {
        this.sellers = sellers;
        this.governorates = governorates;
        this.products = products;
        this.offers = offers;
        this.categories = categories;
        this.brands = brands;
        this.catalog = catalog;
        this.properties = properties;
    }

    @Transactional
    public SellerDto apply(Long userId, ApplyRequest req) {
        Seller seller = sellers.findByUserId(userId).orElse(null);
        if (seller != null && seller.getStatus() != SellerStatus.REJECTED) {
            throw ApiException.conflict("already_applied", "Une demande vendeur existe déjà pour ce compte");
        }
        String taxId = req.taxId().replace(" ", "").toUpperCase(Locale.ROOT);
        if (!TAX_ID.matcher(taxId).matches()) {
            throw ApiException.badRequest("invalid_tax_id", "Matricule fiscal invalide (ex. 1234567A/A/M/000)");
        }
        boolean resubmission = seller != null;
        if ((!resubmission || !taxId.equalsIgnoreCase(seller.getTaxId())) && sellers.existsByTaxIdIgnoreCase(taxId)) {
            throw ApiException.conflict("tax_id_taken", "Ce matricule fiscal est déjà enregistré");
        }
        if ((!resubmission || !req.shopName().trim().equalsIgnoreCase(seller.getShopName()))
                && sellers.existsByShopNameIgnoreCase(req.shopName().trim())) {
            throw ApiException.conflict("shop_name_taken", "Ce nom de boutique est déjà utilisé");
        }
        if (!resubmission) {
            seller = new Seller();
            seller.setUserId(userId);
            seller.setCommissionBp(properties.orders().defaultCommissionBp());
        }
        seller.setShopName(req.shopName().trim());
        seller.setLegalName(req.legalName().trim());
        seller.setTaxId(taxId);
        seller.setRneNumber(req.rneNumber().trim());
        seller.setRib(req.rib());
        seller.setPickupAddress(req.pickupAddress().trim());
        seller.setGovernorate(governorates.findById(req.governorateId())
                .orElseThrow(() -> ApiException.badRequest("invalid_governorate", "Gouvernorat inconnu")));
        seller.setStatus(SellerStatus.PENDING);
        seller.setRejectionReason(null);
        return SellerDto.of(sellers.save(seller));
    }

    @Transactional(readOnly = true)
    public SellerDto me(Long userId) {
        return SellerDto.of(sellers.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Compte vendeur")));
    }

    Seller approvedSeller(Long userId) {
        Seller seller = sellers.findByUserId(userId).orElseThrow(() -> ApiException.forbidden("Compte vendeur requis"));
        if (!seller.isApproved()) {
            throw ApiException.forbidden("Votre compte vendeur n'est pas actif");
        }
        return seller;
    }

    @Transactional(readOnly = true)
    public List<SellerOfferDto> offers(Long userId) {
        return offers.findBySellerIdOrderByIdDesc(approvedSeller(userId).getId()).stream()
                .map(SellerOfferDto::of).toList();
    }

    /** Crée une nouvelle fiche produit (soumise à modération) avec l'offre du vendeur. */
    @Transactional
    public SellerOfferDto createProduct(Long userId, CreateProductRequest req) {
        Seller seller = approvedSeller(userId);
        validate(req.offer());
        String ean = req.ean() == null || req.ean().isBlank() ? null : req.ean();
        if (ean != null && products.existsByEan(ean)) {
            throw ApiException.conflict("ean_exists",
                    "Ce produit existe déjà dans le catalogue : ajoutez simplement votre offre");
        }
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> ApiException.badRequest("invalid_category", "Catégorie inconnue"));

        Product product = new Product();
        product.setCategory(category);
        product.setBrand(resolveBrand(req.brandName()));
        product.setTitle(req.title().trim());
        product.setDescription(req.description().trim());
        product.setEan(ean);
        product.setImageUrl(req.imageUrl() == null || req.imageUrl().isBlank() ? null : req.imageUrl().trim());
        product.setVatBp(category.getVatBp());
        product.setStatus(ProductStatus.PENDING_REVIEW);
        product.setCreatedBySellerId(seller.getId());
        products.save(product);

        Offer offer = new Offer();
        offer.setProduct(product);
        offer.setSeller(seller);
        applyFields(offer, req.offer());
        offers.save(offer);
        catalog.refreshPricing(product);
        return SellerOfferDto.of(offer);
    }

    /** Ajoute l'offre du vendeur sur un produit déjà publié. */
    @Transactional
    public SellerOfferDto attachOffer(Long userId, Long productId, OfferFields fields) {
        Seller seller = approvedSeller(userId);
        validate(fields);
        Product product = products.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Produit"));
        if (offers.existsByProductIdAndSellerId(productId, seller.getId())) {
            throw ApiException.conflict("offer_exists", "Vous avez déjà une offre sur ce produit");
        }
        Offer offer = new Offer();
        offer.setProduct(product);
        offer.setSeller(seller);
        applyFields(offer, fields);
        offers.save(offer);
        catalog.refreshPricing(product);
        return SellerOfferDto.of(offer);
    }

    @Transactional
    public SellerOfferDto updateOffer(Long userId, Long offerId, OfferFields fields, boolean active) {
        Seller seller = approvedSeller(userId);
        validate(fields);
        Offer offer = offers.findByIdAndSellerId(offerId, seller.getId()).orElseThrow(() -> ApiException.notFound("Offre"));
        applyFields(offer, fields);
        offer.setActive(active);
        catalog.refreshPricing(offer.getProduct());
        return SellerOfferDto.of(offer);
    }

    private void validate(OfferFields fields) {
        if (fields.compareAtPrice() != null && fields.compareAtPrice() <= fields.price()) {
            throw ApiException.badRequest("invalid_compare_price", "Le prix barré doit être supérieur au prix de vente");
        }
    }

    private void applyFields(Offer offer, OfferFields fields) {
        offer.setPrice(fields.price());
        offer.setCompareAtPrice(fields.compareAtPrice());
        offer.setStock(fields.stock());
        offer.setHandlingDays(fields.handlingDays());
    }

    private Brand resolveBrand(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return brands.findByNameIgnoreCase(name.trim()).orElseGet(() -> {
            Brand brand = new Brand();
            brand.setName(name.trim());
            return brands.save(brand);
        });
    }
}
