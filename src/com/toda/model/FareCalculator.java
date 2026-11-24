// src/com/toda/model/FareCalculator.java
package com.toda.model;

import java.util.List;

/**
 * Contract for anything that can price itself. RegularTrip and SpecialTrip implement it
 * differently, and callers never need to know which one they are holding.
 */
public interface FareCalculator {
    /** Total fare in pesos, rounded to the centavo. */
    double calculateFare();

    /** Itemised receipt lines that add up to {@link #calculateFare()}. */
    List<FareLine> getFareBreakdown();
}
