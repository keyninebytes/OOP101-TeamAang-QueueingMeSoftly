// src/com/toda/util/RequestBody.java
package com.toda.util;

import com.toda.exception.InvalidInputException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed, validating view over a parsed JSON request body. Every getter turns a missing or
 * malformed field into an {@link InvalidInputException}, so bad input becomes a clean 400.
 */
public class RequestBody {
    private final Map<String, Object> fields;

    @SuppressWarnings("unchecked")
    public RequestBody(String raw) throws InvalidInputException {
        Object parsed;
        try {
            parsed = Json.parse(raw);
        } catch (RuntimeException ex) {
            throw new InvalidInputException("Request body is not valid JSON.");
        }
        if (!(parsed instanceof Map)) {
            throw new InvalidInputException("Request body must be a JSON object.");
        }
        this.fields = (Map<String, Object>) parsed;
    }

    public RequestBody(Map<String, Object> fields) {
        this.fields = fields == null ? new LinkedHashMap<>() : fields;
    }

    public boolean has(String key) {
        return fields.containsKey(key) && fields.get(key) != null;
    }

    public String getString(String key) throws InvalidInputException {
        String value = optString(key, null);
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("'" + key + "' is required.");
        }
        return value.trim();
    }

    public String optString(String key, String fallback) {
        Object v = fields.get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Double d && d == Math.rint(d)) {
            return String.valueOf(d.longValue());
        }
        return String.valueOf(v).trim();
    }

    public double getDouble(String key) throws InvalidInputException {
        if (!has(key)) {
            throw new InvalidInputException("'" + key + "' is required.");
        }
        return toDouble(key, fields.get(key));
    }

    public double optDouble(String key, double fallback) throws InvalidInputException {
        return has(key) ? toDouble(key, fields.get(key)) : fallback;
    }

    public int getInt(String key) throws InvalidInputException {
        double d = getDouble(key);
        return toInt(key, d);
    }

    public int optInt(String key, int fallback) throws InvalidInputException {
        return has(key) ? toInt(key, toDouble(key, fields.get(key))) : fallback;
    }

    public boolean optBoolean(String key, boolean fallback) {
        Object v = fields.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return fallback;
    }

    private static double toDouble(String key, Object v) throws InvalidInputException {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            double d = Double.parseDouble(String.valueOf(v).trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new NumberFormatException();
            }
            return d;
        } catch (NumberFormatException ex) {
            throw new InvalidInputException("'" + key + "' must be a number.");
        }
    }

    private static int toInt(String key, double d) throws InvalidInputException {
        if (d != Math.rint(d)) {
            throw new InvalidInputException("'" + key + "' must be a whole number.");
        }
        return (int) d;
    }
}
