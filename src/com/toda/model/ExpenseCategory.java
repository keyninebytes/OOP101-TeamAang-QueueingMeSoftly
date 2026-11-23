// src/com/toda/model/ExpenseCategory.java
package com.toda.model;

import com.toda.exception.InvalidInputException;

/** Shift expense categories deducted from a driver's gross earnings. */
public enum ExpenseCategory {
    FUEL("Gasoline/Fuel"),
    BOUNDARY("Boundary Fee"),
    TERMINAL("Terminal Fee"),
    MEALS("Meals/Misc");

    private final String label;

    ExpenseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Accepts either the enum name ("FUEL") or its label ("Gasoline/Fuel"). */
    public static ExpenseCategory parse(String raw) throws InvalidInputException {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("Expense category is required.");
        }
        String v = raw.trim();
        for (ExpenseCategory c : values()) {
            if (c.name().equalsIgnoreCase(v) || c.label.equalsIgnoreCase(v)) {
                return c;
            }
        }
        throw new InvalidInputException("Unknown expense category '" + raw + "'.");
    }
}
