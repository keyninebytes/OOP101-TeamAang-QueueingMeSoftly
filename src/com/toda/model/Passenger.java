// src/com/toda/model/Passenger.java
package com.toda.model;

import com.toda.exception.InvalidInputException;

import java.util.Map;

/**
 * A boarding party at the terminal (one person or a group travelling together).
 * discountCount is how many of them are Senior Citizen, Student or PWD.
 */
public class Passenger extends Person {
    private String destination;
    private int partySize;
    private int discountCount;

    public Passenger(String id, String name, String destination, int partySize, int discountCount)
            throws InvalidInputException {
        super(id, (name == null || name.isBlank()) ? "Walk-in party" : name, "");
        setDestination(destination);
        setPartySize(partySize);
        setDiscountCount(discountCount);
    }

    @Override
    public String getDetails() {
        int regular = partySize - discountCount;
        return getName() + " → " + destination + " (" + regular + " regular, " + discountCount + " discounted)";
    }

    @Override
    public String getRole() {
        return "Passenger";
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) throws InvalidInputException {
        if (destination == null || destination.isBlank()) {
            throw new InvalidInputException("Destination is required.");
        }
        this.destination = destination.trim();
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) throws InvalidInputException {
        if (partySize < 1) {
            throw new InvalidInputException("Party size must be at least 1.");
        }
        if (partySize > 40) {
            throw new InvalidInputException("Party size of " + partySize + " is too large for one booking.");
        }
        if (partySize < discountCount) {
            throw new InvalidInputException("Party size cannot be smaller than the discounted count.");
        }
        this.partySize = partySize;
    }

    public int getDiscountCount() {
        return discountCount;
    }

    public void setDiscountCount(int discountCount) throws InvalidInputException {
        if (discountCount < 0) {
            throw new InvalidInputException("Discounted passengers cannot be negative.");
        }
        if (discountCount > partySize) {
            throw new InvalidInputException("Discounted passengers (" + discountCount
                    + ") cannot exceed the party size (" + partySize + ").");
        }
        this.discountCount = discountCount;
    }

    public int getRegularCount() {
        return partySize - discountCount;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = super.toJson();
        m.put("destination", destination);
        m.put("partySize", partySize);
        m.put("discountCount", discountCount);
        m.put("regularCount", getRegularCount());
        return m;
    }
}
