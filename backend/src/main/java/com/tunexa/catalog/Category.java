package com.tunexa.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;
    private String slug;
    private String nameFr;
    private String nameAr;
    private String nameEn;
    /** Taux de TVA par défaut des produits de la catégorie, en points de base (1900 = 19 %). */
    private int vatBp;
    private int sortOrder;
}
