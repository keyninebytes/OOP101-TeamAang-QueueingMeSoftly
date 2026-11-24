// src/com/toda/model/TripRecord.java
package com.toda.model;

import com.toda.util.Csv;
import com.toda.util.JsonSerializable;
import com.toda.util.Money;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable line of the dispatch log, as written to and read back from the CSV files. */
public final class TripRecord implements JsonSerializable {
    public static final String CSV_HEADER =
            "TripID,Timestamp,BodyNo,DriverName,Destination,RegularPax,DiscountedPax,TripType,TotalFare";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String tripId;
    private final LocalDateTime timestamp;
    private final String bodyNumber;
    private final String driverName;
    private final String destination;
    private final int regularPax;
    private final int discountedPax;
    private final String tripType;
    private final double totalFare;

    public TripRecord(String tripId, LocalDateTime timestamp, String bodyNumber, String driverName,
                      String destination, int regularPax, int discountedPax, String tripType, double totalFare) {
        this.tripId = tripId;
        this.timestamp = timestamp;
        this.bodyNumber = bodyNumber;
        this.driverName = driverName;
        this.destination = destination;
        this.regularPax = regularPax;
        this.discountedPax = discountedPax;
        this.tripType = tripType;
        this.totalFare = Money.round(totalFare);
    }

    /** Parses one CSV row. Returns null for a malformed row so one bad line never kills the log. */
    public static TripRecord fromCsv(String line) {
        try {
            List<String> c = Csv.split(line);
            if (c.size() < 9) {
                return null;
            }
            return new TripRecord(c.get(0), LocalDateTime.parse(c.get(1), TS), c.get(2), c.get(3), c.get(4),
                    Integer.parseInt(c.get(5).trim()), Integer.parseInt(c.get(6).trim()), c.get(7),
                    Double.parseDouble(c.get(8).trim()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public String toCsvRow() {
        return Csv.join(tripId, timestamp.format(TS), bodyNumber, driverName, destination,
                String.valueOf(regularPax), String.valueOf(discountedPax), tripType,
                String.format("%.2f", totalFare));
    }

    public String toTxtLine() {
        return String.format("[%s] %s | %s (%s) → %s | Regular: %d | Discounted: %d | %s | %s",
                timestamp.format(TS), tripId, bodyNumber, driverName, destination,
                regularPax, discountedPax, tripType, Money.format(totalFare));
    }

    public String getTripId() {
        return tripId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getBodyNumber() {
        return bodyNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDestination() {
        return destination;
    }

    public int getRegularPax() {
        return regularPax;
    }

    public int getDiscountedPax() {
        return discountedPax;
    }

    public int getTotalPax() {
        return regularPax + discountedPax;
    }

    public String getTripType() {
        return tripType;
    }

    public double getTotalFare() {
        return totalFare;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tripId", tripId);
        m.put("timestamp", timestamp.format(TS));
        m.put("date", timestamp.toLocalDate().toString());
        m.put("bodyNumber", bodyNumber);
        m.put("driverName", driverName);
        m.put("destination", destination);
        m.put("regularPax", regularPax);
        m.put("discountedPax", discountedPax);
        m.put("totalPax", getTotalPax());
        m.put("tripType", tripType);
        m.put("totalFare", totalFare);
        return m;
    }
}
