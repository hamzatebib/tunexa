package com.tunexa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

class OrderFlowTest extends ApiTestSupport {

    private long productId(String query) throws Exception {
        return read(getJson("/api/products?q=" + query, null)).get("items").get(0).get("id").asLong();
    }

    private JsonNode product(long id) throws Exception {
        return read(getJson("/api/products/" + id, null));
    }

    private long defaultAddressId(String token) throws Exception {
        return read(getJson("/api/me/addresses", token)).get(0).get("id").asLong();
    }

    @Test
    void searchFiltersAndSortsProducts() throws Exception {
        getJson("/api/products?q=samsung", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Samsung Galaxy A15 128 Go - Noir"));

        JsonNode electronics = read(getJson("/api/categories", null)).get(0);
        assertThat(electronics.get("slug").asText()).isEqualTo("high-tech");
        JsonNode inHighTech = read(getJson("/api/products?sort=price_asc&category=" + electronics.get("id").asLong(), null));
        assertThat(inHighTech.get("totalItems").asInt()).isGreaterThanOrEqualTo(6);
        long previous = 0;
        for (JsonNode item : inHighTech.get("items")) {
            assertThat(item.get("price").asLong()).isGreaterThanOrEqualTo(previous);
            previous = item.get("price").asLong();
        }

        JsonNode inStockOnly = read(getJson("/api/products?inStock=true&maxPrice=1300000&q=hp", null));
        assertThat(inStockOnly.get("totalItems").asInt()).isZero();
    }

    @Test
    void buyBoxPicksCheapestSellerAndListsOthers() throws Exception {
        JsonNode detail = product(productId("friteuse"));
        assertThat(detail.get("buyBox").get("shopName").asText()).isEqualTo("TechZone Tunis");
        assertThat(detail.get("buyBox").get("price").asLong()).isEqualTo(319_000);
        assertThat(detail.get("otherOffers")).hasSize(1);
        assertThat(detail.get("breadcrumb").get(0).get("slug").asText()).isEqualTo("electromenager");
    }

    @Test
    void customerOrdersWithCashOnDeliveryAndSellerShips() throws Exception {
        String customer = login(CUSTOMER_PHONE, "Client12345");
        long productId = productId("redmi");
        JsonNode offer = product(productId).get("buyBox");
        String seller = login(SELLER_PHONE, "Vendeur12345");
        int stockBefore = sellerStock(seller, offer.get("id").asLong());

        postJson("/api/cart/items", customer, Map.of("offerId", offer.get("id").asLong(), "quantity", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.checkoutAllowed").value(true));

        long addressId = defaultAddressId(customer);
        // Ariana (zone 1) : 2 x 799,000 = 1598 DT, au-dessus du seuil de livraison gratuite
        getJson("/api/checkout/quote?addressId=" + addressId, customer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsTotal").value(1_598_000))
                .andExpect(jsonPath("$.shippingTotal").value(0))
                .andExpect(jsonPath("$.stampDuty").value(1000))
                .andExpect(jsonPath("$.total").value(1_599_000));

        JsonNode placed = read(postJson("/api/checkout", customer,
                Map.of("addressId", addressId, "paymentMethod", "COD")).andExpect(status().isCreated()));
        String number = placed.get("order").get("number").asText();
        assertThat(number).startsWith("TX");
        assertThat(placed.get("order").get("status").asText()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(read(getJson("/api/cart", customer)).get("itemCount").asInt()).isZero();
        assertThat(sellerStock(seller, offer.get("id").asLong())).isEqualTo(stockBefore - 2);

        // Le vendeur ne voit pas encore les commandes non confirmées
        assertThat(packagesFor(seller, number)).isNull();

        postJson("/api/orders/" + number + "/confirm", customer,
                Map.of("code", placed.get("devConfirmationCode").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        JsonNode pkg = packagesFor(seller, number);
        assertThat(pkg.get("amountToCollect").asLong()).isEqualTo(1_599_000);
        assertThat(pkg.get("commission").asLong()).isEqualTo(159_800);
        long packageId = pkg.get("id").asLong();

        postJson("/api/seller/packages/" + packageId + "/ship", seller, Map.of("trackingNumber", "RP123"))
                .andExpect(status().isConflict());
        postJson("/api/seller/packages/" + packageId + "/accept", seller, null).andExpect(status().isOk());
        postJson("/api/seller/packages/" + packageId + "/ship", seller, Map.of("trackingNumber", "RP123456TN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        postJson("/api/orders/" + number + "/cancel", customer, Map.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("already_shipped"));

        String admin = login(ADMIN_PHONE, "Admin12345");
        postJson("/api/admin/packages/" + packageId + "/delivery", admin, Map.of("status", "DELIVERED"))
                .andExpect(status().isOk());

        JsonNode detail = read(getJson("/api/orders/" + number, customer));
        assertThat(detail.get("status").asText()).isEqualTo("DELIVERED");
        assertThat(detail.get("packages").get(0).get("trackingNumber").asText()).isEqualTo("RP123456TN");
        assertThat(detail.get("events").size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void cancellingRestoresStockAndMultiSellerOrderSplitsIntoPackages() throws Exception {
        String customer = login(CUSTOMER_PHONE, "Client12345");
        long robot = product(productId("robot")).get("buyBox").get("id").asLong();
        long yoga = product(productId("yoga")).get("buyBox").get("id").asLong();
        postJson("/api/cart/items", customer, Map.of("offerId", robot, "quantity", 1)).andExpect(status().isOk());
        postJson("/api/cart/items", customer, Map.of("offerId", yoga, "quantity", 1)).andExpect(status().isOk());

        long addressId = defaultAddressId(customer);
        JsonNode quote = read(getJson("/api/checkout/quote?addressId=" + addressId, customer));
        assertThat(quote.get("packages")).hasSize(2);
        // 389 + 49,9 = 438,9 DT d'articles : au-dessus du seuil de 300 DT, livraison offerte
        assertThat(quote.get("shippingTotal").asLong()).isZero();

        JsonNode placed = read(postJson("/api/checkout", customer, Map.of("addressId", addressId, "paymentMethod", "COD")));
        String number = placed.get("order").get("number").asText();
        assertThat(placed.get("order").get("packages")).hasSize(2);
        String seller = login(SELLER_PHONE, "Vendeur12345");
        int yogaStock = sellerStock(seller, yoga);

        postJson("/api/orders/" + number + "/cancel", customer, Map.of("reason", "Erreur de commande"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(sellerStock(seller, yoga)).isEqualTo(yogaStock + 1);
    }

    @Test
    void chargesShippingBelowThresholdAndRefusesEmptyCart() throws Exception {
        String customer = login(CUSTOMER_PHONE, "Client12345");
        long addressId = defaultAddressId(customer);
        postJson("/api/checkout", customer, Map.of("addressId", addressId, "paymentMethod", "COD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("empty_cart"));

        long oil = product(productId("olive")).get("buyBox").get("id").asLong();
        postJson("/api/cart/items", customer, Map.of("offerId", oil, "quantity", 2)).andExpect(status().isOk());
        getJson("/api/checkout/quote?addressId=" + addressId, customer)
                .andExpect(jsonPath("$.shippingTotal").value(7000))
                .andExpect(jsonPath("$.total").value(2 * 23_900 + 7000 + 1000));
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/cart/items/" + oil),
                customer, null).andExpect(status().isOk());
    }

    private int sellerStock(String sellerToken, long offerId) throws Exception {
        for (JsonNode o : read(getJson("/api/seller/offers", sellerToken))) {
            if (o.get("offerId").asLong() == offerId) {
                return o.get("stock").asInt();
            }
        }
        throw new AssertionError("Offre introuvable");
    }

    private JsonNode packagesFor(String sellerToken, String orderNumber) throws Exception {
        for (JsonNode p : read(getJson("/api/seller/packages", sellerToken)).get("items")) {
            if (p.get("orderNumber").asText().equals(orderNumber)) {
                return p;
            }
        }
        return null;
    }
}
