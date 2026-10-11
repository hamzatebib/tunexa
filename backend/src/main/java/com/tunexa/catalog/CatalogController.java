package com.tunexa.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.catalog.CatalogDtos.BrandDto;
import com.tunexa.catalog.CatalogDtos.CategoryNode;
import com.tunexa.catalog.CatalogDtos.ProductCard;
import com.tunexa.catalog.CatalogDtos.ProductDetail;
import com.tunexa.catalog.CatalogDtos.SearchParams;
import com.tunexa.common.PageResponse;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryNode> categories() {
        return catalog.categoryTree();
    }

    @GetMapping("/brands")
    public List<BrandDto> brands() {
        return catalog.brands();
    }

    /** Recherche et liste de produits. Les prix des filtres sont en millimes. */
    @GetMapping("/products")
    public PageResponse<ProductCard> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long brand,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return catalog.search(new SearchParams(q, category, brand, minPrice, maxPrice, inStock, sort), page, size);
    }

    @GetMapping("/products/{id}")
    public ProductDetail product(@PathVariable Long id, @RequestParam(required = false) Long governorate) {
        return catalog.detail(id, governorate);
    }
}
