// src/com/toda/model/RegularTrip.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.Money;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Shared ride priced per head: regular passengers pay the base fare, discounted ones get the statutory rate off. */
public class RegularTrip extends Trip {
    private final double discountRate;

    public RegularTrip(String tripId, LocalDateTime timestamp, String bodyNumber, String driverName,
                       String destination, int regularPax, int discountedPax, double baseFare,
                       double discountRate) throws InvalidInputException {
        super(tripId, timestamp, bodyNumber, driverName, destination, regularPax, discountedPax, baseFare);
        if (discountRate < 0 || discountRate > 1) {
            throw new InvalidInputException("Discount rate must be between 0% and 100%.");
        }
        this.discountRate = discountRate;
    }

    public double getDiscountedUnitFare() {
        return Money.round(getBaseFare() * (1 - discountRate));
    }

    @Override
    public double calculateFare() {
        return Money.round(getRegularPax() * getBaseFare() + getDiscountedPax() * getDiscountedUnitFare());
    }

    @Override
    public List<FareLine> getFareBreakdown() {
        List<FareLine> lines = new ArrayList<>();
        if (getRegularPax() > 0) {
            lines.add(new FareLine("Regular", getRegularPax(), getBaseFare(),
                    Money.round(getRegularPax() * getBaseFare())));
        }
        if (getDiscountedPax() > 0) {
            int pct = (int) Math.round(discountRate * 100);
            lines.add(new FareLine("Senior / Student / PWD (" + pct + "% off)", getDiscountedPax(),
                    getDiscountedUnitFare(), Money.round(getDiscountedPax() * getDiscountedUnitFare())));
        }
        return lines;
    }

    @Override
    public String getTripType() {
        return "REGULAR";
    }
}
