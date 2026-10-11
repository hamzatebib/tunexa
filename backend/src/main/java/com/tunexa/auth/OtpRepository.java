package com.tunexa.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {

    long countByTargetAndPurposeAndCreatedAtAfter(String target, OtpPurpose purpose, Instant after);

    Optional<OtpCode> findFirstByTargetAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String target, OtpPurpose purpose);
}
