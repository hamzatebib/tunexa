package com.tunexa.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tunexa.common.ApiException;
import com.tunexa.config.TunexaProperties;

@Service
public class OtpService {

    private final OtpRepository repository;
    private final SmsSender smsSender;
    private final PasswordEncoder encoder;
    private final TunexaProperties.Otp config;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpRepository repository, SmsSender smsSender, PasswordEncoder encoder, TunexaProperties properties) {
        this.repository = repository;
        this.smsSender = smsSender;
        this.encoder = encoder;
        this.config = properties.otp();
    }

    /**
     * Génère un code à 6 chiffres pour {@code target} et l'envoie par SMS à {@code phone}.
     * Renvoie le code uniquement si {@code tunexa.otp.expose-code} est activé (développement).
     */
    @Transactional
    public Optional<String> send(String target, OtpPurpose purpose, String phone, String messagePrefix) {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        if (repository.countByTargetAndPurposeAndCreatedAtAfter(target, purpose, oneHourAgo) >= config.maxSendsPerHour()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "otp_rate_limited",
                    "Trop de codes envoyés, réessayez dans une heure");
        }
        repository.findFirstByTargetAndPurposeAndConsumedFalseOrderByCreatedAtDesc(target, purpose)
                .ifPresent(previous -> previous.setConsumed(true));

        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpCode otp = new OtpCode();
        otp.setTarget(target);
        otp.setPurpose(purpose);
        otp.setCodeHash(encoder.encode(code));
        otp.setExpiresAt(Instant.now().plus(config.ttl()));
        repository.save(otp);

        smsSender.send(phone, messagePrefix + " " + code + " (valable " + config.ttl().toMinutes() + " min)");
        return config.exposeCode() ? Optional.of(code) : Optional.empty();
    }

    /** Vérifie et consomme le code. Le compteur de tentatives est enregistré même en cas d'échec. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public void verify(String target, OtpPurpose purpose, String code) {
        OtpCode otp = repository.findFirstByTargetAndPurposeAndConsumedFalseOrderByCreatedAtDesc(target, purpose)
                .orElseThrow(() -> ApiException.badRequest("otp_expired", "Code expiré, demandez un nouveau code"));
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("otp_expired", "Code expiré, demandez un nouveau code");
        }
        if (otp.getAttempts() >= config.maxAttempts()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "otp_locked",
                    "Trop de tentatives, demandez un nouveau code");
        }
        if (code == null || !encoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            throw ApiException.badRequest("otp_invalid", "Code incorrect");
        }
        otp.setConsumed(true);
    }
}
