package com.tunexa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

class SellerOnboardingTest extends ApiTestSupport {

    @Test
    void newSellerIsValidatedThenPublishesAModeratedProduct() throws Exception {
        String phone = "+21650111222";
        String code = read(postJson("/api/auth/otp", null, Map.of("phone", phone, "purpose", "REGISTER")))
                .get("devCode").asText();
        String token = read(postJson("/api/auth/register", null, Map.of("phone", phone, "code", code,
                "firstName", "Hedi", "lastName", "Mansour", "password", "Boutique123", "acceptTerms", true)))
                .get("token").asText();

        long governorate = read(getJson("/api/geo/governorates", null)).get(0).get("id").asLong();
        Map<String, Object> application = Map.of("shopName", "Sahel Shop", "legalName", "Sahel Shop SARL",
                "taxId", "1112223C/A/M/000", "rneNumber", "B123456", "rib", "12345678901234567890",
                "pickupAddress", "Route de la plage, Sousse", "governorateId", governorate);
        postJson("/api/seller/apply", token, Map.of("shopName", "X", "legalName", "X", "taxId", "123",
                "rneNumber", "1", "rib", "12345678901234567890", "pickupAddress", "x", "governorateId", governorate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_tax_id"));
        JsonNode applied = read(postJson("/api/seller/apply", token, application).andExpect(status().isCreated()));
        assertThat(applied.get("status").asText()).isEqualTo("PENDING");

        // Tant que le dossier n'est pas validé, l'espace vendeur reste fermé
        getJson("/api/seller/offers", token).andExpect(status().isForbidden());

        String admin = login(ADMIN_PHONE, "Admin12345");
        postJson("/api/admin/sellers/" + applied.get("id").asLong() + "/approve", admin, null)
                .andExpect(status().isOk());
        String sellerToken = read(postJson("/api/me/token", token, null)).get("token").asText();

        long category = read(getJson("/api/categories", null)).get(0).get("children").get(0).get("id").asLong();
        Map<String, Object> product = Map.of("categoryId", category, "brandName", "Nokia",
                "title", "Nokia 105 Dual SIM", "description", "Téléphone simple et robuste, autonomie record.",
                "ean", "6438409012345",
                "offer", Map.of("price", 59_000, "compareAtPrice", 65_000, "stock", 30, "handlingDays", 1));
        JsonNode created = read(postJson("/api/seller/products", sellerToken, product).andExpect(status().isCreated()));
        assertThat(created.get("productStatus").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(read(getJson("/api/products?q=nokia", null)).get("totalItems").asInt()).isZero();

        postJson("/api/seller/products", sellerToken, product)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ean_exists"));

        JsonNode pending = read(getJson("/api/admin/products/pending", admin));
        assertThat(pending.get(0).get("sellerShopName").asText()).isEqualTo("Sahel Shop");
        postJson("/api/admin/products/" + created.get("productId").asLong() + "/approve", admin, null)
                .andExpect(status().isOk());

        JsonNode found = read(getJson("/api/products?q=nokia", null));
        assertThat(found.get("totalItems").asInt()).isEqualTo(1);
        assertThat(found.get("items").get(0).get("price").asLong()).isEqualTo(59_000);

        putJson("/api/seller/offers/" + created.get("offerId").asLong(), sellerToken,
                Map.of("offer", Map.of("price", 55_000, "stock", 0, "handlingDays", 2), "active", true))
                .andExpect(status().isOk());
        JsonNode outOfStock = read(getJson("/api/products?q=nokia", null)).get("items").get(0);
        assertThat(outOfStock.get("inStock").asBoolean()).isFalse();
    }
}
