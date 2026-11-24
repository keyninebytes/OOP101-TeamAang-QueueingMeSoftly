// src/com/toda/model/Trip.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.JsonSerializable;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A dispatched (or quoted) tricycle run. The shared facts live here; how the fare is priced is
 * left abstract so {@link RegularTrip} and {@link SpecialTrip} can each apply their own rules.
 */
public abstract class Trip implements FareCalculator, JsonSerializable {
    private final String tripId;
    private final LocalDateTime timestamp;
    private final String bodyNumber;
    private final String driverName;
    private final String destination;
    private final int regularPax;
    private final int discountedPax;
    private final double baseFare;

    protected Trip(String tripId, LocalDateTime timestamp, String bodyNumber, String driverName,
                   String destination, int regularPax, int discountedPax, double baseFare)
            throws InvalidInputException {
        if (regularPax < 0 || discountedPax < 0) {
            throw new InvalidInputException("Passenger counts cannot be negative.");
        }
        if (baseFare <= 0) {
            throw new InvalidInputException("Base fare must be greater than ₱0.00.");
        }
        if (destination == null || destination.isBlank()) {
            throw new InvalidInputException("A trip needs a destination.");
        }
        this.tripId = tripId;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.bodyNumber = bodyNumber;
        this.driverName = driverName;
        this.destination = destination;
        this.regularPax = regularPax;
        this.discountedPax = discountedPax;
        this.baseFare = baseFare;
    }

    /** "REGULAR" or "SPECIAL" — written to the dispatch log. */
    public abstract String getTripType();

    /** Freezes this trip into an immutable log record. Fare comes from the subclass. */
    public TripRecord toRecord() {
        return new TripRecord(tripId, timestamp, bodyNumber, driverName, destination,
                regularPax, discountedPax, getTripType(), calculateFare());
    }

    public String getTripId() {
        return tripId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getBodyNumber() {
        return bodyNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDestination() {
        return destination;
    }

    public int getRegularPax() {
        return regularPax;
    }

    public int getDiscountedPax() {
        return discountedPax;
    }

    public int getTotalPax() {
        return regularPax + discountedPax;
    }

    public double getBaseFare() {
        return baseFare;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tripId", tripId);
        m.put("timestamp", timestamp.toString());
        m.put("bodyNumber", bodyNumber);
        m.put("driverName", driverName);
        m.put("destination", destination);
        m.put("regularPax", regularPax);
        m.put("discountedPax", discountedPax);
        m.put("tripType", getTripType());
        m.put("baseFare", baseFare);
        m.put("totalFare", calculateFare());
        m.put("breakdown", getFareBreakdown());
        return m;
    }
}
