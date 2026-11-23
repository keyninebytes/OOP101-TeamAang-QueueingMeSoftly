// src/com/toda/model/Destination.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.JsonSerializable;
import com.toda.util.Money;

import java.util.LinkedHashMap;
import java.util.Map;

/** A terminal route with its per-passenger base fare. */
public class Destination implements JsonSerializable {
    private final String id;
    private String name;
    private double baseFare;
    private boolean active;

    public Destination(String id, String name, double baseFare, boolean active) throws InvalidInputException {
        if (id == null || id.isBlank()) {
            throw new InvalidInputException("Destination ID is required.");
        }
        this.id = id;
        setName(name);
        setBaseFare(baseFare);
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidInputException {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Destination name is required.");
        }
        String clean = name.trim().replaceAll("\\s+", " ");
        if (clean.length() > 40) {
            throw new InvalidInputException("Destination name must be 40 characters or fewer.");
        }
        this.name = clean;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) throws InvalidInputException {
        if (Double.isNaN(baseFare) || baseFare <= 0) {
            throw new InvalidInputException("Base fare must be greater than ₱0.00.");
        }
        if (baseFare > 5000) {
            throw new InvalidInputException("Base fare cannot exceed ₱5,000.00.");
        }
        this.baseFare = Money.round(baseFare);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Builds a URL-safe id from a name, e.g. "Batangas Pier" → "batangas-pier". */
    public static String slugify(String name) {
        String slug = name == null ? "" : name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "route" : slug;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("baseFare", baseFare);
        m.put("active", active);
        return m;
    }
}
