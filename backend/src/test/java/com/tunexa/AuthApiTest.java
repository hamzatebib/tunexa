package com.tunexa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AuthApiTest extends ApiTestSupport {

    private Map<String, Object> registration(String phone, String code) {
        return Map.of("phone", phone, "code", code, "firstName", "Yassine", "lastName", "Jlassi",
                "password", "MotDePasse1", "language", "ar", "acceptTerms", true);
    }

    @Test
    void registersWithSmsCodeThenLogsIn() throws Exception {
        String code = read(postJson("/api/auth/otp", null, Map.of("phone", "98 123 456", "purpose", "REGISTER"))
                .andExpect(status().isOk())).get("devCode").asText();
        assertThat(code).hasSize(6);

        postJson("/api/auth/register", null, registration("+216 98 123 456", code))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.phone").value("+21698123456"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.language").value("ar"));

        String token = login("98123456", "MotDePasse1");
        getJson("/api/me", token).andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Yassine"));
    }

    @Test
    void rejectsWrongCodeAndLocksAfterTooManyAttempts() throws Exception {
        String phone = "+21697000001";
        postJson("/api/auth/otp", null, Map.of("phone", phone, "purpose", "REGISTER")).andExpect(status().isOk());
        for (int i = 0; i < 5; i++) {
            postJson("/api/auth/register", null, registration(phone, "000000"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("otp_invalid"));
        }
        postJson("/api/auth/register", null, registration(phone, "000000"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("otp_locked"));
    }

    @Test
    void rejectsInvalidPhoneAndDuplicateRegistration() throws Exception {
        postJson("/api/auth/otp", null, Map.of("phone", "0612345678", "purpose", "REGISTER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_phone"));
        postJson("/api/auth/otp", null, Map.of("phone", CUSTOMER_PHONE, "purpose", "REGISTER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("phone_taken"));
    }

    @Test
    void limitsSmsSendsPerHour() throws Exception {
        String phone = "+21697000002";
        for (int i = 0; i < 3; i++) {
            postJson("/api/auth/otp", null, Map.of("phone", phone, "purpose", "REGISTER")).andExpect(status().isOk());
        }
        postJson("/api/auth/otp", null, Map.of("phone", phone, "purpose", "REGISTER"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void resetsPassword() throws Exception {
        String phone = "+21623333333";
        String code = read(postJson("/api/auth/otp", null, Map.of("phone", phone, "purpose", "RESET_PASSWORD")))
                .get("devCode").asText();
        postJson("/api/auth/reset-password", null, Map.of("phone", phone, "code", code, "newPassword", "Nouveau12345"))
                .andExpect(status().isNoContent());
        postJson("/api/auth/login", null, Map.of("phone", phone, "password", "Vendeur12345"))
                .andExpect(status().isUnauthorized());
        assertThat(login(phone, "Nouveau12345")).isNotBlank();
    }

    @Test
    void protectsPrivateEndpoints() throws Exception {
        getJson("/api/cart", null).andExpect(status().isUnauthorized());
        getJson("/api/admin/stats", login(CUSTOMER_PHONE, "Client12345")).andExpect(status().isForbidden());
        getJson("/api/seller/offers", login(CUSTOMER_PHONE, "Client12345")).andExpect(status().isForbidden());
    }
}
