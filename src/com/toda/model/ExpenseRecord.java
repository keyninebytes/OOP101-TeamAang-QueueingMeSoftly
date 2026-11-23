// src/com/toda/model/ExpenseRecord.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.JsonSerializable;
import com.toda.util.Money;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable expense entry. Validation happens once, in the constructor. */
public final class ExpenseRecord implements JsonSerializable {
    private final String id;
    private final String driverId;
    private final ExpenseCategory category;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String note;

    public ExpenseRecord(String id, String driverId, ExpenseCategory category, double amount,
                         LocalDateTime timestamp, String note) throws InvalidInputException {
        if (category == null) {
            throw new InvalidInputException("Expense category is required.");
        }
        if (Double.isNaN(amount) || amount <= 0) {
            throw new InvalidInputException("Expense amount must be greater than ₱0.00.");
        }
        if (amount > 100_000) {
            throw new InvalidInputException("Expense amount looks too large. Please double-check it.");
        }
        this.id = id;
        this.driverId = driverId;
        this.category = category;
        this.amount = Money.round(amount);
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        String n = note == null ? "" : note.trim();
        this.note = n.length() > 120 ? n.substring(0, 120) : n;
    }

    public String getId() {
        return id;
    }

    public String getDriverId() {
        return driverId;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getNote() {
        return note;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("driverId", driverId);
        m.put("category", category.name());
        m.put("categoryLabel", category.getLabel());
        m.put("amount", amount);
        m.put("timestamp", timestamp.toString());
        m.put("note", note);
        return m;
    }
}
