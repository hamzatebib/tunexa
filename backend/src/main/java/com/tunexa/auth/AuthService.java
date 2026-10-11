package com.tunexa.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.auth.AuthDtos.AuthResponse;
import com.tunexa.auth.AuthDtos.OtpResponse;
import com.tunexa.auth.AuthDtos.RegisterRequest;
import com.tunexa.common.ApiException;
import com.tunexa.config.JwtService;
import com.tunexa.user.Role;
import com.tunexa.user.User;
import com.tunexa.user.UserDto;
import com.tunexa.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final OtpService otpService;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, OtpService otpService, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.otpService = otpService;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public OtpResponse requestOtp(String rawPhone, OtpPurpose purpose) {
        String phone = PhoneNumbers.normalize(rawPhone);
        boolean exists = users.existsByPhone(phone);
        Optional<String> devCode = switch (purpose) {
            case REGISTER -> {
                if (exists) {
                    throw ApiException.conflict("phone_taken", "Ce numéro est déjà inscrit, connectez-vous");
                }
                yield otpService.send(phone, purpose, phone, "Tunexa : votre code d'inscription est");
            }
            // On ne révèle pas si le numéro est inscrit : la réponse est identique dans les deux cas
            case RESET_PASSWORD -> exists
                    ? otpService.send(phone, purpose, phone, "Tunexa : votre code de réinitialisation est")
                    : Optional.empty();
            case ORDER_CONFIRMATION -> throw ApiException.badRequest("invalid_purpose", "Usage non autorisé");
        };
        return new OtpResponse(true, devCode.orElse(null));
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String phone = PhoneNumbers.normalize(req.phone());
        if (users.existsByPhone(phone)) {
            throw ApiException.conflict("phone_taken", "Ce numéro est déjà inscrit, connectez-vous");
        }
        otpService.verify(phone, OtpPurpose.REGISTER, req.code());

        User user = new User();
        user.setPhone(phone);
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setLanguage(req.language() == null ? "fr" : req.language());
        user.setRole(Role.CUSTOMER);
        user.setTermsAcceptedAt(Instant.now());
        users.save(user);
        return new AuthResponse(jwtService.issue(user), UserDto.of(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String rawPhone, String password) {
        String phone = PhoneNumbers.normalize(rawPhone);
        User user = users.findByPhone(phone)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "bad_credentials",
                        "Numéro ou mot de passe incorrect"));
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled",
                    "Compte suspendu, contactez le service client");
        }
        return new AuthResponse(jwtService.issue(user), UserDto.of(user));
    }

    @Transactional
    public void resetPassword(String rawPhone, String code, String newPassword) {
        String phone = PhoneNumbers.normalize(rawPhone);
        otpService.verify(phone, OtpPurpose.RESET_PASSWORD, code);
        User user = users.findByPhone(phone).orElseThrow(() -> ApiException.notFound("Compte"));
        user.setPasswordHash(encoder.encode(newPassword));
    }
}
