package com.tunexa.common;

/** Les montants sont stockés en millimes (1 TND = 1000 millimes). */
public final class Money {

    private Money() {
    }

    /** Applique un taux exprimé en points de base (1000 = 10 %), arrondi au millime. */
    public static long percentOf(long amount, int basisPoints) {
        return Math.round(amount * basisPoints / 10_000.0);
    }

    /** Part de TVA contenue dans un montant TTC. */
    public static long vatIncluded(long amountInclTax, int vatBp) {
        return Math.round(amountInclTax - amountInclTax * 10_000.0 / (10_000 + vatBp));
    }
}
