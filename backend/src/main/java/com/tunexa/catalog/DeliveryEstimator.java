package com.tunexa.catalog;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

/**
 * Estimation de la date de livraison : délai de préparation du vendeur + transit selon la zone
 * (1 = Grand Tunis, 2 = Nord / Centre / Sahel, 3 = Sud). Le dimanche n'est pas ouvré.
 */
@Component
public class DeliveryEstimator {

    public record Estimate(LocalDate from, LocalDate to) {
    }

    private final Clock clock;

    public DeliveryEstimator() {
        this(Clock.systemDefaultZone());
    }

    DeliveryEstimator(Clock clock) {
        this.clock = clock;
    }

    public Estimate estimate(int handlingDays, int zone) {
        int transitMin = switch (zone) {
            case 1 -> 1;
            case 2 -> 2;
            default -> 3;
        };
        LocalDate today = LocalDate.now(clock);
        return new Estimate(addWorkingDays(today, handlingDays + transitMin),
                addWorkingDays(today, handlingDays + transitMin + 2));
    }

    static LocalDate addWorkingDays(LocalDate start, int days) {
        LocalDate date = start;
        int added = 0;
        while (added < days) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return date;
    }
}
