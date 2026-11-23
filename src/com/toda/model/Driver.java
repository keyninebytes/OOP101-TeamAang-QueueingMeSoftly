// src/com/toda/model/Driver.java
package com.toda.model;

import com.toda.exception.InvalidInputException;
import com.toda.util.Money;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A registered TODA driver and their tricycle. Money fields can only change through
 * {@link #addEarnings(double)} and {@link #recordExpense(String, double)}, so the net profit
 * can never drift from the records that produced it.
 */
public class Driver extends Person {
    private String licenseNumber;
    private String assignedTricycleBodyNo;
    private DriverStatus status;
    private double grossEarnings;
    private double totalExpenses;
    private int tripsCompleted;
    private int passengersServed;
    private final List<ExpenseRecord> expenses = new ArrayList<>();

    public Driver(String id, String name, String contactNumber, String licenseNumber,
                  String assignedTricycleBodyNo) throws InvalidInputException {
        super(id, name, contactNumber);
        setLicenseNumber(licenseNumber);
        setAssignedTricycleBodyNo(assignedTricycleBodyNo);
        this.status = DriverStatus.ACTIVE;
    }

    @Override
    public String getDetails() {
        return getName() + " (" + assignedTricycleBodyNo + ", " + status.getLabel() + ")";
    }

    @Override
    public String getRole() {
        return "Driver";
    }

    // ------------------------------------------------------------ money

    /** Adds a completed trip's fare to gross earnings. Negative fares are rejected. */
    public void addEarnings(double amount) throws InvalidInputException {
        if (Double.isNaN(amount) || amount < 0) {
            throw new InvalidInputException("Earnings cannot be negative.");
        }
        grossEarnings = Money.round(grossEarnings + amount);
    }

    /** Books a completed dispatch: fare, trip count and passengers in one step. */
    public void completeTrip(double fare, int passengers) throws InvalidInputException {
        if (passengers < 0) {
            throw new InvalidInputException("Passenger count cannot be negative.");
        }
        addEarnings(fare);
        tripsCompleted++;
        passengersServed += passengers;
    }

    /** Convenience overload matching the proposal: category text + amount. */
    public ExpenseRecord recordExpense(String category, double amount) throws InvalidInputException {
        ExpenseRecord record = new ExpenseRecord("EXP-" + System.nanoTime(), getId(),
                ExpenseCategory.parse(category), amount, LocalDateTime.now(), "");
        recordExpense(record);
        return record;
    }

    /** Adds a pre-built expense record (used by the service and when loading saved state). */
    public void recordExpense(ExpenseRecord record) throws InvalidInputException {
        if (record == null) {
            throw new InvalidInputException("Expense record is required.");
        }
        expenses.add(record);
        totalExpenses = Money.round(totalExpenses + record.getAmount());
    }

    public double getGrossEarnings() {
        return grossEarnings;
    }

    public double getTotalExpenses() {
        return totalExpenses;
    }

    /** Net Profit = Gross Earnings - Total Expenses. */
    public double getNetProfit() {
        return Money.round(grossEarnings - totalExpenses);
    }

    /** Read-only view: callers cannot add or remove expenses behind the driver's back. */
    public List<ExpenseRecord> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    public int getTripsCompleted() {
        return tripsCompleted;
    }

    public int getPassengersServed() {
        return passengersServed;
    }

    /** Restores counters from saved state; validated like any other mutation. */
    public void restoreTotals(double gross, int trips, int passengers) throws InvalidInputException {
        if (gross < 0 || trips < 0 || passengers < 0) {
            throw new InvalidInputException("Saved driver totals cannot be negative.");
        }
        this.grossEarnings = Money.round(gross);
        this.tripsCompleted = trips;
        this.passengersServed = passengers;
    }

    // ------------------------------------------------------------ fleet details

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) throws InvalidInputException {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new InvalidInputException("License number is required.");
        }
        String clean = licenseNumber.trim().toUpperCase();
        if (!clean.matches("[A-Z0-9\\-]{5,20}")) {
            throw new InvalidInputException("License number must be 5 to 20 letters, digits or dashes (e.g. D04-12-345678).");
        }
        this.licenseNumber = clean;
    }

    public String getAssignedTricycleBodyNo() {
        return assignedTricycleBodyNo;
    }

    public void setAssignedTricycleBodyNo(String bodyNo) throws InvalidInputException {
        this.assignedTricycleBodyNo = normalizeBodyNumber(bodyNo);
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) throws InvalidInputException {
        if (status == null) {
            throw new InvalidInputException("Status is required.");
        }
        this.status = status;
    }

    /** Normalises "t1", "T-1" and "t-001" to "T-001". Rejects empty or malformed numbers. */
    public static String normalizeBodyNumber(String raw) throws InvalidInputException {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("Tricycle body number cannot be empty.");
        }
        String v = raw.trim().toUpperCase().replace(" ", "");
        if (v.matches("T-?\\d{1,4}")) {
            int n = Integer.parseInt(v.replaceAll("\\D", ""));
            if (n == 0) {
                throw new InvalidInputException("Body number must be T-001 or higher.");
            }
            return String.format("T-%03d", n);
        }
        if (!v.matches("[A-Z0-9\\-]{2,10}")) {
            throw new InvalidInputException("Body number must look like T-001.");
        }
        return v;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = super.toJson();
        m.put("licenseNumber", licenseNumber);
        m.put("bodyNumber", assignedTricycleBodyNo);
        m.put("status", status.name());
        m.put("statusLabel", status.getLabel());
        m.put("grossEarnings", grossEarnings);
        m.put("totalExpenses", totalExpenses);
        m.put("netProfit", getNetProfit());
        m.put("tripsCompleted", tripsCompleted);
        m.put("passengersServed", passengersServed);
        return m;
    }
}
