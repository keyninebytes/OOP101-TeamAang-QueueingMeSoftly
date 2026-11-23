// src/com/toda/model/DriverStatus.java
package com.toda.model;

import com.toda.exception.InvalidInputException;

/** Fleet statuses. Only ACTIVE drivers may stand in the dispatch line. */
public enum DriverStatus {
    ACTIVE("Active", true),
    ON_BREAK("On Break", false),
    MAINTENANCE("Maintenance", false),
    INACTIVE("Inactive", false);

    private final String label;
    private final boolean canQueue;

    DriverStatus(String label, boolean canQueue) {
        this.label = label;
        this.canQueue = canQueue;
    }

    public String getLabel() {
        return label;
    }

    public boolean canQueue() {
        return canQueue;
    }

    public static DriverStatus parse(String raw) throws InvalidInputException {
        if (raw == null) {
            throw new InvalidInputException("Status is required.");
        }
        String key = raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        for (DriverStatus s : values()) {
            if (s.name().equals(key)) {
                return s;
            }
        }
        throw new InvalidInputException("Unknown status '" + raw + "'. Use ACTIVE, ON_BREAK, MAINTENANCE or INACTIVE.");
    }
}
