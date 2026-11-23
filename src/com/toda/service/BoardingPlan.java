// src/com/toda/service/BoardingPlan.java
package com.toda.service;

import com.toda.model.FareLine;
import com.toda.util.JsonSerializable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of smart matching for one party. When the party does not fit the first eligible unit,
 * the plan carries both a "split" option and a "whole" (next unit with enough seats) option so
 * the dispatcher can choose inline.
 */
public class BoardingPlan implements JsonSerializable {
    private final String destinationId;
    private final String destinationName;
    private final int partySize;
    private final int discounted;
    private final List<FareLine> receipt;
    private final double totalFare;
    private final List<SeatAllocation> wholeOption;
    private final List<SeatAllocation> splitOption;
    private final String note;

    public BoardingPlan(String destinationId, String destinationName, int partySize, int discounted,
                        List<FareLine> receipt, double totalFare, List<SeatAllocation> wholeOption,
                        List<SeatAllocation> splitOption, String note) {
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.partySize = partySize;
        this.discounted = discounted;
        this.receipt = List.copyOf(receipt);
        this.totalFare = totalFare;
        this.wholeOption = wholeOption == null ? null : List.copyOf(wholeOption);
        this.splitOption = splitOption == null ? null : List.copyOf(splitOption);
        this.note = note;
    }

    /** The first eligible unit has room for everyone, so no question is needed. */
    public boolean fitsFirstUnit() {
        return wholeOption != null && splitOption == null;
    }

    public boolean choiceRequired() {
        return wholeOption != null && splitOption != null;
    }

    public List<SeatAllocation> getWholeOption() {
        return wholeOption;
    }

    public List<SeatAllocation> getSplitOption() {
        return splitOption;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public double getTotalFare() {
        return totalFare;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("destinationId", destinationId);
        m.put("destinationName", destinationName);
        m.put("partySize", partySize);
        m.put("discounted", discounted);
        m.put("regular", partySize - discounted);
        m.put("receipt", new ArrayList<>(receipt));
        m.put("totalFare", totalFare);
        m.put("wholeOption", wholeOption);
        m.put("splitOption", splitOption);
        m.put("choiceRequired", choiceRequired());
        m.put("recommended", wholeOption != null && splitOption == null ? "whole" : (splitOption != null ? "split" : "whole"));
        m.put("note", note);
        return m;
    }
}
