// src/com/toda/model/QueuedTricycle.java
package com.toda.model;

import com.toda.exception.CapacityExceededException;
import com.toda.exception.InvalidInputException;
import com.toda.util.JsonSerializable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tricycle standing in the terminal line. It guards its own seats: boarding past
 * {@code maxCapacity}, mixing destinations, or boarding a locked special trip all throw.
 */
public class QueuedTricycle implements JsonSerializable {
    public enum QueueStatus { IN_QUEUE, BOARDING, SPECIAL_TRIP }

    private final String bodyNumber;
    private final String driverId;
    private final String driverName;
    private final LocalDateTime joinedAt;
    private int maxCapacity;
    private String destinationId;
    private String destinationName;
    private int regularPax;
    private int discountedPax;
    private boolean specialTrip;
    private int specialPartySize;
    private final List<Passenger> parties = new ArrayList<>();

    public QueuedTricycle(Driver driver, int maxCapacity, LocalDateTime joinedAt) throws InvalidInputException {
        if (driver == null) {
            throw new InvalidInputException("A queued tricycle needs a driver.");
        }
        this.bodyNumber = driver.getAssignedTricycleBodyNo();
        this.driverId = driver.getId();
        this.driverName = driver.getName();
        this.joinedAt = joinedAt == null ? LocalDateTime.now() : joinedAt;
        setMaxCapacity(maxCapacity);
    }

    // ------------------------------------------------------------ boarding

    /** True when this unit can take {@code seats} more passengers bound for the destination. */
    public boolean canAccept(String destId, int seats) {
        return !specialTrip && servesDestination(destId) && getRemainingSeats() >= seats;
    }

    /** True when the unit is unassigned or already heading to the destination. */
    public boolean servesDestination(String destId) {
        return destinationId == null || destinationId.equals(destId);
    }

    /** Boards a party (or one slice of a split party). */
    public void board(Passenger party, String destId, String destName)
            throws CapacityExceededException, InvalidInputException {
        if (party == null) {
            throw new InvalidInputException("Nothing to board.");
        }
        if (specialTrip) {
            throw new InvalidInputException(bodyNumber + " is locked as a special trip.");
        }
        if (!servesDestination(destId)) {
            throw new InvalidInputException(bodyNumber + " is already heading to " + destinationName + ".");
        }
        if (party.getPartySize() > getRemainingSeats()) {
            throw new CapacityExceededException(bodyNumber + " has only " + getRemainingSeats()
                    + " seat(s) left but the party needs " + party.getPartySize() + ".");
        }
        this.destinationId = destId;
        this.destinationName = destName;
        this.regularPax += party.getRegularCount();
        this.discountedPax += party.getDiscountCount();
        parties.add(party);
    }

    /** Locks the unit as a private/special hire. Only allowed while no shared passengers are aboard. */
    public void enableSpecialTrip(String destId, String destName, int partySize) throws InvalidInputException {
        if (getCurrentPassengers() > 0 && !specialTrip) {
            throw new InvalidInputException(bodyNumber + " already has shared passengers. Clear them before a special trip.");
        }
        if (destId == null || destName == null) {
            throw new InvalidInputException("A special trip needs a destination.");
        }
        if (partySize < 1 || partySize > 20) {
            throw new InvalidInputException("Special trip party size must be between 1 and 20.");
        }
        this.specialTrip = true;
        this.specialPartySize = partySize;
        this.destinationId = destId;
        this.destinationName = destName;
    }

    public void disableSpecialTrip() {
        if (specialTrip) {
            specialTrip = false;
            specialPartySize = 0;
            destinationId = null;
            destinationName = null;
        }
    }

    /** Unloads everyone and frees the destination. */
    public void clearPassengers() {
        regularPax = 0;
        discountedPax = 0;
        parties.clear();
        disableSpecialTrip();
        destinationId = null;
        destinationName = null;
    }

    /** Keeps the cached destination name in sync when a route is renamed. */
    public void renameDestination(String destId, String newName) {
        if (destId != null && destId.equals(destinationId)) {
            destinationName = newName;
        }
    }

    // ------------------------------------------------------------ getters

    public String getBodyNumber() {
        return bodyNumber;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) throws InvalidInputException {
        if (maxCapacity < 1 || maxCapacity > 10) {
            throw new InvalidInputException("Max capacity must be between 1 and 10 seats.");
        }
        if (maxCapacity < regularPax + discountedPax) {
            throw new InvalidInputException("Max capacity cannot be lower than the passengers already aboard.");
        }
        this.maxCapacity = maxCapacity;
    }

    public String getDestinationId() {
        return destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public int getRegularPax() {
        return regularPax;
    }

    public int getDiscountedPax() {
        return discountedPax;
    }

    public int getCurrentPassengers() {
        return specialTrip ? specialPartySize : regularPax + discountedPax;
    }

    public int getRemainingSeats() {
        return specialTrip ? 0 : maxCapacity - (regularPax + discountedPax);
    }

    public boolean isSpecialTrip() {
        return specialTrip;
    }

    public int getSpecialPartySize() {
        return specialPartySize;
    }

    public boolean isFull() {
        return getRemainingSeats() == 0;
    }

    public double getFillRatio() {
        return specialTrip ? 1.0 : (double) (regularPax + discountedPax) / maxCapacity;
    }

    public List<Passenger> getParties() {
        return Collections.unmodifiableList(parties);
    }

    public QueueStatus getQueueStatus() {
        if (specialTrip) {
            return QueueStatus.SPECIAL_TRIP;
        }
        return getCurrentPassengers() > 0 ? QueueStatus.BOARDING : QueueStatus.IN_QUEUE;
    }

    /** Restores seat counts from saved state (validated). */
    public void restoreSeats(String destId, String destName, int regular, int discounted,
                             boolean special, int specialSize) throws InvalidInputException {
        if (regular < 0 || discounted < 0 || (!special && regular + discounted > maxCapacity)) {
            throw new InvalidInputException("Saved seat counts for " + bodyNumber + " are invalid.");
        }
        this.destinationId = destId;
        this.destinationName = destName;
        this.regularPax = special ? 0 : regular;
        this.discountedPax = special ? 0 : discounted;
        this.specialTrip = special;
        this.specialPartySize = special ? Math.max(1, specialSize) : 0;
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bodyNumber", bodyNumber);
        m.put("driverId", driverId);
        m.put("driverName", driverName);
        m.put("joinedAt", joinedAt.toString());
        m.put("maxCapacity", maxCapacity);
        m.put("destinationId", destinationId);
        m.put("destinationName", destinationName);
        m.put("regularPax", regularPax);
        m.put("discountedPax", discountedPax);
        m.put("currentPassengers", getCurrentPassengers());
        m.put("remainingSeats", getRemainingSeats());
        m.put("specialTrip", specialTrip);
        m.put("specialPartySize", specialPartySize);
        m.put("status", getQueueStatus().name());
        List<Map<String, Object>> partyJson = new ArrayList<>();
        for (Passenger p : parties) {
            partyJson.add(p.toJson());
        }
        m.put("parties", partyJson);
        return m;
    }
}
