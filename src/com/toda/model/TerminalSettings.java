// src/com/toda/model/TerminalSettings.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.JsonSerializable;
import com.toda.util.Money;
import com.toda.util.RequestBody;

import java.util.LinkedHashMap;
import java.util.Map;

/** System and display preferences. Every setter validates; updates are applied all-or-nothing. */
public class TerminalSettings implements JsonSerializable {
    public enum SpecialMode { MULTIPLIER, FLAT }

    private String terminalName = "Calumpang TODA Terminal";
    private int defaultMaxCapacity = 4;
    private double discountRate = 0.20;
    private SpecialMode specialMode = SpecialMode.MULTIPLIER;
    private double specialMultiplier = 4.0;
    private double specialFlatRate = 80.0;
    private boolean autoRouteSorting = false;
    private String theme = "system";
    private String colorPrimary = "#0A84FF";
    private String colorSecondary = "#5E5CE6";
    private String colorAccent = "#30D158";

    public TerminalSettings copy() {
        TerminalSettings c = new TerminalSettings();
        c.terminalName = terminalName;
        c.defaultMaxCapacity = defaultMaxCapacity;
        c.discountRate = discountRate;
        c.specialMode = specialMode;
        c.specialMultiplier = specialMultiplier;
        c.specialFlatRate = specialFlatRate;
        c.autoRouteSorting = autoRouteSorting;
        c.theme = theme;
        c.colorPrimary = colorPrimary;
        c.colorSecondary = colorSecondary;
        c.colorAccent = colorAccent;
        return c;
    }

    /**
     * Returns a new settings object with the supplied fields changed. The original is untouched
     * if any field is invalid, so a bad form never leaves settings half-saved.
     */
    public TerminalSettings withUpdates(RequestBody body) throws InvalidInputException {
        TerminalSettings next = copy();
        if (body.has("terminalName")) {
            next.setTerminalName(body.optString("terminalName", ""));
        }
        if (body.has("defaultMaxCapacity")) {
            next.setDefaultMaxCapacity(body.optInt("defaultMaxCapacity", defaultMaxCapacity));
        }
        if (body.has("discountPercent")) {
            next.setDiscountRate(body.optDouble("discountPercent", discountRate * 100) / 100.0);
        }
        if (body.has("specialMode")) {
            next.setSpecialMode(body.optString("specialMode", "MULTIPLIER"));
        }
        if (body.has("specialMultiplier")) {
            next.setSpecialMultiplier(body.optDouble("specialMultiplier", specialMultiplier));
        }
        if (body.has("specialFlatRate")) {
            next.setSpecialFlatRate(body.optDouble("specialFlatRate", specialFlatRate));
        }
        if (body.has("autoRouteSorting")) {
            next.autoRouteSorting = body.optBoolean("autoRouteSorting", autoRouteSorting);
        }
        if (body.has("theme")) {
            next.setTheme(body.optString("theme", "system"));
        }
        if (body.has("colorPrimary")) {
            next.colorPrimary = validHex(body.optString("colorPrimary", ""), "Primary");
        }
        if (body.has("colorSecondary")) {
            next.colorSecondary = validHex(body.optString("colorSecondary", ""), "Secondary");
        }
        if (body.has("colorAccent")) {
            next.colorAccent = validHex(body.optString("colorAccent", ""), "Accent");
        }
        return next;
    }

    private static String validHex(String raw, String which) throws InvalidInputException {
        String v = raw == null ? "" : raw.trim();
        if (!v.matches("#[0-9a-fA-F]{6}")) {
            throw new InvalidInputException(which + " color must be a hex value like #0A84FF.");
        }
        return v.toUpperCase();
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) throws InvalidInputException {
        if (terminalName == null || terminalName.isBlank()) {
            throw new InvalidInputException("Terminal name cannot be empty.");
        }
        if (terminalName.trim().length() > 50) {
            throw new InvalidInputException("Terminal name must be 50 characters or fewer.");
        }
        this.terminalName = terminalName.trim();
    }

    public int getDefaultMaxCapacity() {
        return defaultMaxCapacity;
    }

    public void setDefaultMaxCapacity(int capacity) throws InvalidInputException {
        if (capacity < 1 || capacity > 10) {
            throw new InvalidInputException("Default max capacity must be between 1 and 10.");
        }
        this.defaultMaxCapacity = capacity;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double rate) throws InvalidInputException {
        if (Double.isNaN(rate) || rate < 0 || rate > 1) {
            throw new InvalidInputException("Discount must be between 0% and 100%.");
        }
        this.discountRate = Math.round(rate * 10000) / 10000.0;
    }

    public SpecialMode getSpecialMode() {
        return specialMode;
    }

    public void setSpecialMode(String mode) throws InvalidInputException {
        try {
            this.specialMode = SpecialMode.valueOf(mode.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new InvalidInputException("Special trip mode must be MULTIPLIER or FLAT.");
        }
    }

    public double getSpecialMultiplier() {
        return specialMultiplier;
    }

    public void setSpecialMultiplier(double m) throws InvalidInputException {
        if (Double.isNaN(m) || m < 1 || m > 20) {
            throw new InvalidInputException("Special trip multiplier must be between 1× and 20×.");
        }
        this.specialMultiplier = Math.round(m * 100) / 100.0;
    }

    public double getSpecialFlatRate() {
        return specialFlatRate;
    }

    public void setSpecialFlatRate(double rate) throws InvalidInputException {
        if (Double.isNaN(rate) || rate <= 0 || rate > 10000) {
            throw new InvalidInputException("Special trip flat rate must be between ₱0.01 and ₱10,000.00.");
        }
        this.specialFlatRate = Money.round(rate);
    }

    public boolean isAutoRouteSorting() {
        return autoRouteSorting;
    }

    public void setAutoRouteSorting(boolean on) {
        this.autoRouteSorting = on;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) throws InvalidInputException {
        String t = theme == null ? "" : theme.trim().toLowerCase();
        if (!t.equals("light") && !t.equals("dark") && !t.equals("system")) {
            throw new InvalidInputException("Theme must be light, dark or system.");
        }
        this.theme = t;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("terminalName", terminalName);
        m.put("defaultMaxCapacity", defaultMaxCapacity);
        m.put("discountPercent", Math.round(discountRate * 10000) / 100.0);
        m.put("specialMode", specialMode.name());
        m.put("specialMultiplier", specialMultiplier);
        m.put("specialFlatRate", specialFlatRate);
        m.put("autoRouteSorting", autoRouteSorting);
        m.put("theme", theme);
        m.put("colorPrimary", colorPrimary);
        m.put("colorSecondary", colorSecondary);
        m.put("colorAccent", colorAccent);
        return m;
    }
}
