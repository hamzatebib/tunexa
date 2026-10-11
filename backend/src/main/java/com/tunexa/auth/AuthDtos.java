package com.tunexa.auth;

import com.tunexa.user.UserDto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record OtpRequest(@NotBlank String phone, @NotNull OtpPurpose purpose) {
    }

    /** {@code devCode} n'est renseigné qu'en développement. */
    public record OtpResponse(boolean sent, String devCode) {
    }

    public record RegisterRequest(
            @NotBlank String phone,
            @NotBlank @Size(min = 6, max = 6) String code,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Size(min = 8, max = 72, message = "8 caractères minimum") String password,
            @Pattern(regexp = "fr|ar|en") String language,
            @AssertTrue(message = "Vous devez accepter les conditions générales") boolean acceptTerms) {
    }

    public record LoginRequest(@NotBlank String phone, @NotBlank String password) {
    }

    public record ResetPasswordRequest(
            @NotBlank String phone,
            @NotBlank String code,
            @NotBlank @Size(min = 8, max = 72, message = "8 caractères minimum") String newPassword) {
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
