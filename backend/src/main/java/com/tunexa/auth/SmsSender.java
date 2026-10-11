package com.tunexa.auth;

/** Point d'intégration du fournisseur SMS (Ooredoo, Orange, Tunisie Télécom via un agrégateur). */
public interface SmsSender {

    void send(String phone, String message);
}
