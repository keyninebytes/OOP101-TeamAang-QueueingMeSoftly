// src/com/toda/model/SpecialTrip.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.Money;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Private hire. Seat counts and discounts do not matter: the whole tricycle is billed either a
 * flat rate or the route's base fare times a multiplier, depending on terminal settings.
 */
public class SpecialTrip extends Trip {
    private final boolean flatRateMode;
    private final double multiplier;
    private final double flatRate;

    public SpecialTrip(String tripId, LocalDateTime timestamp, String bodyNumber, String driverName,
                       String destination, int passengers, double baseFare, boolean flatRateMode,
                       double multiplier, double flatRate) throws InvalidInputException {
        super(tripId, timestamp, bodyNumber, driverName, destination, passengers, 0, baseFare);
        if (flatRateMode && flatRate <= 0) {
            throw new InvalidInputException("Special trip flat rate must be greater than ₱0.00.");
        }
        if (!flatRateMode && multiplier < 1) {
            throw new InvalidInputException("Special trip multiplier must be at least 1×.");
        }
        this.flatRateMode = flatRateMode;
        this.multiplier = multiplier;
        this.flatRate = flatRate;
    }

    @Override
    public double calculateFare() {
        return Money.round(flatRateMode ? flatRate : getBaseFare() * multiplier);
    }

    @Override
    public List<FareLine> getFareBreakdown() {
        String label = flatRateMode
                ? "Special trip (flat rate)"
                : "Special trip (base fare × " + trimNumber(multiplier) + ")";
        return List.of(new FareLine(label, 1, calculateFare(), calculateFare()));
    }

    @Override
    public String getTripType() {
        return "SPECIAL";
    }

    private static String trimNumber(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
