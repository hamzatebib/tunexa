package com.tunexa.catalog;

import com.tunexa.seller.Seller;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/** Offre d'un vendeur sur un produit : prix, stock et délai de préparation propres au vendeur. */
@Entity
@Table(name = "offers")
@Getter
@Setter
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    /** Prix TTC en millimes. */
    private long price;
    private Long compareAtPrice;
    private int stock;
    private int maxPerOrder = 10;
    private int handlingDays = 1;

    @Column(name = "item_condition")
    private String condition = "NEW";

    private boolean active = true;

    @Version
    private long version;

    public boolean isBuyable() {
        return active && stock > 0 && seller.isApproved() && product.getStatus() == ProductStatus.PUBLISHED;
    }
}
