package com.tunexa.demo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.address.Address;
import com.tunexa.address.AddressRepository;
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
import com.tunexa.config.TunexaProperties;
import com.tunexa.geo.Governorate;
import com.tunexa.geo.GovernorateRepository;
import com.tunexa.seller.Seller;
import com.tunexa.seller.SellerRepository;
import com.tunexa.seller.SellerStatus;
import com.tunexa.user.Role;
import com.tunexa.user.User;
import com.tunexa.user.UserRepository;

/** Données de démonstration (activées par tunexa.demo-data=true, jamais en production). */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private record DemoProduct(String category, String brand, String title, String description, long price,
            Long compareAt, int stock, int sellerIndex, Long secondSellerPrice) {
    }

    private static final List<DemoProduct> PRODUCTS = List.of(
            new DemoProduct("smartphones", "Samsung", "Samsung Galaxy A15 128 Go - Noir",
                    "Écran Super AMOLED 6,5\", 4 Go de RAM, 128 Go de stockage, batterie 5000 mAh, double SIM.",
                    549_000, 629_000L, 25, 0, 559_000L),
            new DemoProduct("smartphones", "Xiaomi", "Xiaomi Redmi Note 13 256 Go - Bleu",
                    "Écran AMOLED 120 Hz, appareil photo 108 MP, charge rapide 33 W, 8 Go de RAM.",
                    799_000, null, 12, 0, null),
            new DemoProduct("smartphones", "Apple", "Apple iPhone 15 128 Go - Rose",
                    "Puce A16 Bionic, Dynamic Island, appareil photo 48 MP, USB-C. Garantie 1 an.",
                    3_999_000, 4_299_000L, 5, 0, null),
            new DemoProduct("ordinateurs", "Lenovo", "Lenovo IdeaPad Slim 3 15\" i5 8 Go 512 Go SSD",
                    "Processeur Intel Core i5 de 12e génération, écran Full HD 15,6\", Windows 11.",
                    1_689_000, 1_899_000L, 8, 0, 1_719_000L),
            new DemoProduct("ordinateurs", "HP", "HP 250 G9 15,6\" i3 8 Go 256 Go SSD",
                    "Ordinateur portable professionnel, clavier AZERTY, Wi-Fi 6.",
                    1_249_000, null, 0, 0, null),
            new DemoProduct("tv-audio", "TCL", "TV TCL 43\" 4K UHD Smart Google TV",
                    "Téléviseur 43 pouces 4K HDR, Google TV, Dolby Audio, récepteur intégré.",
                    999_000, 1_149_000L, 10, 0, null),
            new DemoProduct("gros-electromenager", "Condor", "Réfrigérateur Condor 350 L No Frost - Inox",
                    "Réfrigérateur combiné No Frost, classe énergétique A+, garantie 2 ans.",
                    1_590_000, null, 4, 1, null),
            new DemoProduct("gros-electromenager", "Brandt", "Machine à laver Brandt 8 kg 1200 tr/min",
                    "Lave-linge frontal 8 kg, 15 programmes, départ différé.",
                    1_399_000, 1_499_000L, 6, 1, null),
            new DemoProduct("petit-electromenager", "Moulinex", "Robot pâtissier Moulinex 1000 W",
                    "Bol inox 4,8 L, 6 vitesses, crochet, fouet et batteur inclus.",
                    389_000, 449_000L, 15, 1, null),
            new DemoProduct("petit-electromenager", "Tefal", "Friteuse sans huile Tefal Easy Fry 4,2 L",
                    "Cuisson saine sans huile, 8 programmes, panier antiadhésif.",
                    329_000, null, 20, 1, 319_000L),
            new DemoProduct("mode-homme", "Tunexa Basics", "Chemise homme en lin - Blanc",
                    "Chemise 100 % lin, coupe droite, idéale pour l'été.",
                    59_900, 79_900L, 40, 1, null),
            new DemoProduct("mode-femme", "Tunexa Basics", "Robe d'été brodée - Bleu",
                    "Robe légère en coton avec broderies traditionnelles.",
                    89_000, null, 30, 1, null),
            new DemoProduct("beaute", "Garnier", "Coffret soin visage Garnier Vitamine C",
                    "Sérum 30 ml + crème de jour 50 ml, éclat et hydratation.",
                    65_500, 79_000L, 50, 1, null),
            new DemoProduct("epicerie", "Zitouna", "Huile d'olive extra vierge tunisienne 1 L",
                    "Huile d'olive extra vierge de première pression à froid, région de Sfax.",
                    24_500, null, 200, 1, 23_900L),
            new DemoProduct("epicerie", "Dattes Deglet Nour", "Dattes Deglet Nour de Tozeur 1 kg",
                    "Dattes branchées Deglet Nour, récolte de l'année.",
                    16_900, null, 150, 1, null),
            new DemoProduct("artisanat", "Artisans de Nabeul", "Plat en céramique de Nabeul peint main 30 cm",
                    "Plat décoratif en céramique, motifs traditionnels peints à la main.",
                    75_000, null, 12, 1, null),
            new DemoProduct("scolaire", "Bic", "Kit rentrée scolaire primaire",
                    "Cahiers, stylos, crayons de couleur, règle, trousse : tout pour la rentrée.",
                    45_000, 52_000L, 80, 1, null),
            new DemoProduct("sport", "Decathlon", "Tapis de yoga 8 mm antidérapant",
                    "Tapis épais et confortable avec sangle de transport.",
                    49_900, null, 35, 0, null));

    private final TunexaProperties properties;
    private final UserRepository users;
    private final SellerRepository sellers;
    private final GovernorateRepository governorates;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;
    private final OfferRepository offers;
    private final AddressRepository addresses;
    private final CatalogService catalog;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(TunexaProperties properties, UserRepository users, SellerRepository sellers,
            GovernorateRepository governorates, CategoryRepository categories, BrandRepository brands,
            ProductRepository products, OfferRepository offers, AddressRepository addresses, CatalogService catalog,
            PasswordEncoder encoder) {
        this.properties = properties;
        this.users = users;
        this.sellers = sellers;
        this.governorates = governorates;
        this.categories = categories;
        this.brands = brands;
        this.products = products;
        this.offers = offers;
        this.addresses = addresses;
        this.catalog = catalog;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.demoData() || users.count() > 0) {
            return;
        }
        Governorate tunis = governorate("TN-11");
        Governorate sfax = governorate("TN-61");

        user("+21620000000", "Admin", "Tunexa", "Admin12345", Role.ADMIN);
        User customer = user("+21655555555", "Amira", "Ben Salah", "Client12345", Role.CUSTOMER);
        Seller tech = seller(user("+21622222222", "Karim", "Trabelsi", "Vendeur12345", Role.SELLER),
                "TechZone Tunis", "TechZone SARL", "1234567A/A/M/000", tunis, new BigDecimal("4.6"));
        Seller medina = seller(user("+21623333333", "Sonia", "Gharbi", "Vendeur12345", Role.SELLER),
                "Médina Market", "Médina Market SUARL", "7654321B/A/M/000", sfax, new BigDecimal("4.8"));
        List<Seller> demoSellers = List.of(tech, medina);

        Address address = new Address();
        address.setUserId(customer.getId());
        address.setLabel("Domicile");
        address.setFullName("Amira Ben Salah");
        address.setPhone("+21655555555");
        address.setGovernorate(governorate("TN-12"));
        address.setDelegation("La Soukra");
        address.setLocality("Chotrana 2");
        address.setPostalCode("2036");
        address.setStreet("12 rue des Jasmins");
        address.setLandmark("En face de la pharmacie");
        address.setDefaultAddress(true);
        addresses.save(address);

        long minutes = 0;
        for (DemoProduct d : PRODUCTS) {
            Category category = categories.findAll().stream().filter(c -> c.getSlug().equals(d.category()))
                    .findFirst().orElseGet(() -> categories.findAll().stream()
                            .filter(c -> c.getSlug().equals("beaute")).findFirst().orElseThrow());
            Product p = new Product();
            p.setCategory(category);
            p.setBrand(brand(d.brand()));
            p.setTitle(d.title());
            p.setDescription(d.description());
            p.setVatBp(category.getVatBp());
            p.setStatus(ProductStatus.PUBLISHED);
            p.setCreatedAt(Instant.now().minusSeconds(60 * minutes++));
            products.save(p);
            offer(p, demoSellers.get(d.sellerIndex()), d.price(), d.compareAt(), d.stock(), 1);
            if (d.secondSellerPrice() != null) {
                offer(p, demoSellers.get(1 - d.sellerIndex()), d.secondSellerPrice(), null, 7, 2);
            }
            catalog.refreshPricing(p);
        }
        log.info("Données de démonstration créées : admin +21620000000 / Admin12345, client +21655555555 / "
                + "Client12345, vendeur +21622222222 / Vendeur12345");
    }

    private Governorate governorate(String code) {
        return governorates.findAll().stream().filter(g -> g.getCode().equals(code)).findFirst().orElseThrow();
    }

    private User user(String phone, String first, String last, String password, Role role) {
        User u = new User();
        u.setPhone(phone);
        u.setFirstName(first);
        u.setLastName(last);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        u.setTermsAcceptedAt(Instant.now());
        return users.save(u);
    }

    private Seller seller(User user, String shop, String legal, String taxId, Governorate gov, BigDecimal rating) {
        Seller s = new Seller();
        s.setUserId(user.getId());
        s.setShopName(shop);
        s.setLegalName(legal);
        s.setTaxId(taxId);
        s.setRneNumber("B0" + user.getId() + "2024");
        s.setRib("0800" + String.format("%016d", user.getId()));
        s.setPickupAddress("Zone industrielle, " + gov.getNameFr());
        s.setGovernorate(gov);
        s.setStatus(SellerStatus.APPROVED);
        s.setCommissionBp(properties.orders().defaultCommissionBp());
        s.setRating(rating);
        return sellers.save(s);
    }

    private Brand brand(String name) {
        return brands.findByNameIgnoreCase(name).orElseGet(() -> {
            Brand b = new Brand();
            b.setName(name);
            return brands.save(b);
        });
    }

    private void offer(Product p, Seller seller, long price, Long compareAt, int stock, int handlingDays) {
        Offer o = new Offer();
        o.setProduct(p);
        o.setSeller(seller);
        o.setPrice(price);
        o.setCompareAtPrice(compareAt);
        o.setStock(stock);
        o.setHandlingDays(handlingDays);
        offers.save(o);
    }
}
