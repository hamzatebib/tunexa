package com.tunexa.auth;

import java.util.regex.Pattern;

import com.tunexa.common.ApiException;

/** Normalise les numéros de mobile tunisiens au format international +216XXXXXXXX. */
public final class PhoneNumbers {

    private static final Pattern TN_MOBILE = Pattern.compile("^\\+216[2-9]\\d{7}$");

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw ApiException.badRequest("invalid_phone", "Numéro de téléphone requis");
        }
        String digits = raw.replaceAll("[\\s.\\-()]", "");
        if (digits.startsWith("00216")) {
            digits = "+" + digits.substring(2);
        } else if (digits.startsWith("216") && digits.length() == 11) {
            digits = "+" + digits;
        } else if (digits.length() == 8) {
            digits = "+216" + digits;
        }
        if (!TN_MOBILE.matcher(digits).matches()) {
            throw ApiException.badRequest("invalid_phone", "Numéro tunisien invalide (format +216 XX XXX XXX)");
        }
        return digits;
    }
}
