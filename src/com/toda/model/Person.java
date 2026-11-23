// src/com/toda/model/Person.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.JsonSerializable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract base for every human in the terminal. It owns identity and contact validation,
 * while each subclass decides how it describes itself through {@link #getDetails()}.
 */
public abstract class Person implements JsonSerializable {
    private final String id;
    private String name;
    private String contactNumber;

    protected Person(String id, String name, String contactNumber) throws InvalidInputException {
        if (id == null || id.isBlank()) {
            throw new InvalidInputException("An ID is required.");
        }
        this.id = id;
        setName(name);
        setContactNumber(contactNumber);
    }

    /** One-line human description, implemented differently by drivers and passengers. */
    public abstract String getDetails();

    /** Role label used in JSON and logs ("Driver", "Passenger"). */
    public abstract String getRole();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidInputException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Name cannot be empty.");
        }
        String clean = name.trim().replaceAll("\\s+", " ");
        if (clean.length() > 60) {
            throw new InvalidInputException("Name must be 60 characters or fewer.");
        }
        this.name = clean;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    /** Optional. When present it must look like a PH mobile/landline (digits, spaces, +, -). */
    public void setContactNumber(String contactNumber) throws InvalidInputException {
        if (contactNumber == null || contactNumber.isBlank()) {
            this.contactNumber = "";
            return;
        }
        String clean = contactNumber.trim();
        String digits = clean.replaceAll("[\\s+\\-()]", "");
        if (!digits.matches("\\d{7,13}") || !clean.matches("[0-9+\\-()\\s]+")) {
            throw new InvalidInputException("Contact number must have 7 to 13 digits (e.g. 0917 123 4567).");
        }
        this.contactNumber = clean;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("contactNumber", contactNumber);
        m.put("role", getRole());
        m.put("details", getDetails());
        return m;
    }

    @Override
    public String toString() {
        return getDetails();
    }
}
