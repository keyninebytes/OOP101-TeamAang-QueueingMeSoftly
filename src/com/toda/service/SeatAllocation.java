// src/com/toda/service/SeatAllocation.java
package com.toda.service;

import com.toda.util.JsonSerializable;

import java.util.LinkedHashMap;
import java.util.Map;

/** One slice of a boarding plan: how many of the party ride in which tricycle, and what they pay. */
public record SeatAllocation(String bodyNumber, String driverName, int queuePosition, int regular,
                             int discounted, int seatsLeftAfter, double fare) implements JsonSerializable {
    public int seats() {
        return regular + discounted;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bodyNumber", bodyNumber);
        m.put("driverName", driverName);
        m.put("queuePosition", queuePosition);
        m.put("seats", seats());
        m.put("regular", regular);
        m.put("discounted", discounted);
        m.put("seatsLeftAfter", seatsLeftAfter);
        m.put("fare", fare);
        return m;
    }
}
