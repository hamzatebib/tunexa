package com.tunexa.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implémentation par défaut : écrit le SMS dans les logs. Le connecteur du fournisseur réel sera déclaré avec @Primary. */
@Component
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void send(String phone, String message) {
        log.info("[SMS] à {} : {}", phone, message);
    }
}
