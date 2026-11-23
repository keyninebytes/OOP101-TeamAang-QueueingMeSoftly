// src/com/toda/model/FareLine.java
package com.toda.model;

import com.toda.util.JsonSerializable;

import java.util.LinkedHashMap;
import java.util.Map;

/** One line on a receipt preview, e.g. "Student / Senior / PWD (20% off) × 1 @ ₱12.00 = ₱12.00". */
public record FareLine(String label, int quantity, double unitPrice, double amount) implements JsonSerializable {
    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("quantity", quantity);
        m.put("unitPrice", unitPrice);
        m.put("amount", amount);
        return m;
    }
}
