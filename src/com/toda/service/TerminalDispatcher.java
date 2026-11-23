// src/com/toda/service/TerminalDispatcher.java
package com.toda.service;

import com.toda.exception.CapacityExceededException;
import com.toda.exception.DuplicateEntryException;
import com.toda.exception.EmptyQueueException;
import com.toda.exception.InvalidInputException;
import com.toda.exception.NotFoundException;
import com.toda.exception.TerminalException;
import com.toda.model.Destination;
import com.toda.model.Driver;
import com.toda.model.DriverStatus;
import com.toda.model.ExpenseCategory;
import com.toda.model.ExpenseRecord;
import com.toda.model.Passenger;
import com.toda.model.QueuedTricycle;
import com.toda.model.RegularTrip;
import com.toda.model.SpecialTrip;
import com.toda.model.TerminalSettings;
import com.toda.model.Trip;
import com.toda.model.TripRecord;
import com.toda.structure.TricycleQueue;
import com.toda.util.RequestBody;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The terminal's brain. Owns the roster, routes, settings and the FIFO line, and enforces every
 * business rule (who may queue, smart batch boarding, fares, dispatch, expenses). All public
 * methods are synchronized so the HTTP server's worker threads see one consistent terminal.
 * Every successful mutation is persisted immediately so nothing is lost on restart.
 */
public class TerminalDispatcher {
    private final Map<String, Driver> drivers = new LinkedHashMap<>();
    private final Map<String, Destination> destinations = new LinkedHashMap<>();
    private final TricycleQueue queue = new TricycleQueue();
    private final TripLogger logger;
    private final DataStore store;
    private TerminalSettings settings = new TerminalSettings();
    private int driverSeq = 0;
    private int expenseSeq = 0;
    private boolean persistenceEnabled = true;

    public TerminalDispatcher(DataStore store, TripLogger logger) {
        this.store = store;
        this.logger = logger;
    }

    // =====================================================================================
    // Settings
    // =====================================================================================

    public synchronized TerminalSettings getSettings() {
        return settings;
    }

    public synchronized TerminalSettings updateSettings(RequestBody body) throws TerminalException {
        TerminalSettings next = settings.withUpdates(body);
        boolean sortingTurnedOn = next.isAutoRouteSorting() && !settings.isAutoRouteSorting();
        settings = next;
        if (sortingTurnedOn) {
            applyRouteSorting();
        }
        persist();
        return settings;
    }

    // =====================================================================================
    // Destinations
    // =====================================================================================

    public synchronized List<Destination> listDestinations() {
        return new ArrayList<>(destinations.values());
    }

    public synchronized Destination addDestination(RequestBody body) throws TerminalException {
        String name = body.getString("name");
        double fare = body.getDouble("baseFare");
        ensureUniqueDestinationName(name, null);
        String id = Destination.slugify(name);
        String unique = id;
        for (int i = 2; destinations.containsKey(unique); i++) {
            unique = id + "-" + i;
        }
        Destination d = new Destination(unique, name, fare, body.optBoolean("active", true));
        destinations.put(d.getId(), d);
        persist();
        return d;
    }

    public synchronized Destination updateDestination(String id, RequestBody body) throws TerminalException {
        Destination d = requireDestination(id);
        // validate into locals first so a bad field leaves the route untouched
        String name = body.has("name") ? body.getString("name") : d.getName();
        double fare = body.has("baseFare") ? body.getDouble("baseFare") : d.getBaseFare();
        boolean active = body.has("active") ? body.optBoolean("active", d.isActive()) : d.isActive();
        ensureUniqueDestinationName(name, id);
        new Destination(id, name, fare, active); // throws if invalid
        if (!active && d.isActive() && isDestinationInUse(id)) {
            throw new InvalidInputException(d.getName() + " has tricycles boarding for it. Dispatch or clear them before deactivating.");
        }
        d.setName(name);
        d.setBaseFare(fare);
        d.setActive(active);
        for (QueuedTricycle t : queue.toList()) {
            t.renameDestination(id, d.getName());
        }
        persist();
        return d;
    }

    public synchronized void deleteDestination(String id) throws TerminalException {
        Destination d = requireDestination(id);
        if (isDestinationInUse(id)) {
            throw new InvalidInputException(d.getName() + " has tricycles boarding for it. Dispatch or clear them first.");
        }
        destinations.remove(id);
        persist();
    }

    private boolean isDestinationInUse(String id) {
        for (QueuedTricycle t : queue.toList()) {
            if (id.equals(t.getDestinationId())) {
                return true;
            }
        }
        return false;
    }

    private void ensureUniqueDestinationName(String name, String exceptId) throws DuplicateEntryException {
        for (Destination d : destinations.values()) {
            if (!d.getId().equals(exceptId) && d.getName().equalsIgnoreCase(name.trim())) {
                throw new DuplicateEntryException("A route named '" + d.getName() + "' already exists.");
            }
        }
    }

    private Destination requireDestination(String id) throws NotFoundException {
        Destination d = id == null ? null : destinations.get(id);
        if (d == null) {
            throw new NotFoundException("Destination '" + id + "' was not found.");
        }
        return d;
    }

    // =====================================================================================
    // Drivers
    // =====================================================================================

    public synchronized List<Driver> listDrivers() {
        return new ArrayList<>(drivers.values());
    }

    public synchronized Driver getDriver(String id) throws NotFoundException {
        Driver d = drivers.get(id);
        if (d == null) {
            throw new NotFoundException("Driver '" + id + "' was not found.");
        }
        return d;
    }

    public synchronized Driver registerDriver(RequestBody body) throws TerminalException {
        String id = String.format("DRV-%04d", driverSeq + 1);
        Driver d = new Driver(id, body.getString("name"), body.optString("contactNumber", ""),
                body.getString("licenseNumber"), body.getString("bodyNumber"));
        ensureUniqueDriver(d.getLicenseNumber(), d.getAssignedTricycleBodyNo(), null);
        if (body.has("status")) {
            d.setStatus(DriverStatus.parse(body.optString("status", "ACTIVE")));
        }
        driverSeq++;
        drivers.put(d.getId(), d);
        if (body.optBoolean("joinQueue", false) && d.getStatus().canQueue()) {
            queue.enqueue(new QueuedTricycle(d, settings.getDefaultMaxCapacity(), LocalDateTime.now()));
        }
        persist();
        return d;
    }

    public synchronized Driver updateDriver(String id, RequestBody body) throws TerminalException {
        Driver d = getDriver(id);
        String name = body.has("name") ? body.getString("name") : d.getName();
        String contact = body.has("contactNumber") ? body.optString("contactNumber", "") : d.getContactNumber();
        String license = body.has("licenseNumber") ? body.getString("licenseNumber") : d.getLicenseNumber();
        String bodyNo = body.has("bodyNumber") ? body.getString("bodyNumber") : d.getAssignedTricycleBodyNo();
        // dry-run on a scratch object so a bad field leaves the real driver untouched
        Driver check = new Driver(d.getId(), name, contact, license, bodyNo);
        ensureUniqueDriver(check.getLicenseNumber(), check.getAssignedTricycleBodyNo(), id);
        boolean bodyChanged = !check.getAssignedTricycleBodyNo().equals(d.getAssignedTricycleBodyNo());
        boolean nameChanged = !check.getName().equals(d.getName());
        if ((bodyChanged || nameChanged) && queue.findByBodyNumber(d.getAssignedTricycleBodyNo()) != null) {
            throw new InvalidInputException("Take " + d.getAssignedTricycleBodyNo()
                    + " out of the queue before changing its body number or driver name.");
        }
        d.setName(check.getName());
        d.setContactNumber(check.getContactNumber());
        d.setLicenseNumber(check.getLicenseNumber());
        d.setAssignedTricycleBodyNo(check.getAssignedTricycleBodyNo());
        if (body.has("status")) {
            applyStatus(d, DriverStatus.parse(body.optString("status", "")));
        }
        persist();
        return d;
    }

    /**
     * Changes fleet status. Leaving ACTIVE pulls the unit out of the dispatch line, but the
     * driver's earnings and expense history stay untouched.
     */
    public synchronized Map<String, Object> setDriverStatus(String id, String rawStatus) throws TerminalException {
        Driver d = getDriver(id);
        boolean removed = applyStatus(d, DriverStatus.parse(rawStatus));
        persist();
        Map<String, Object> out = new LinkedHashMap<>(d.toJson());
        out.put("removedFromQueue", removed);
        return out;
    }

    private boolean applyStatus(Driver d, DriverStatus status) throws TerminalException {
        QueuedTricycle queued = queue.findByBodyNumber(d.getAssignedTricycleBodyNo());
        if (!status.canQueue() && queued != null && queued.getCurrentPassengers() > 0) {
            throw new InvalidInputException(d.getAssignedTricycleBodyNo() + " still has "
                    + queued.getCurrentPassengers() + " passenger(s) aboard. Dispatch or clear them first.");
        }
        d.setStatus(status);
        if (!status.canQueue() && queued != null) {
            queue.removeByBodyNumber(d.getAssignedTricycleBodyNo());
            return true;
        }
        return false;
    }

    public synchronized void deleteDriver(String id) throws TerminalException {
        Driver d = getDriver(id);
        if (queue.findByBodyNumber(d.getAssignedTricycleBodyNo()) != null) {
            throw new InvalidInputException("Remove " + d.getAssignedTricycleBodyNo() + " from the queue before deleting the driver.");
        }
        drivers.remove(id);
        persist();
    }

    private void ensureUniqueDriver(String license, String bodyNo, String exceptId) throws DuplicateEntryException {
        for (Driver other : drivers.values()) {
            if (other.getId().equals(exceptId)) {
                continue;
            }
            if (other.getAssignedTricycleBodyNo().equalsIgnoreCase(bodyNo)) {
                throw new DuplicateEntryException("Body number " + bodyNo + " is already assigned to " + other.getName() + ".");
            }
            if (other.getLicenseNumber().equalsIgnoreCase(license)) {
                throw new DuplicateEntryException("License " + license + " is already registered to " + other.getName() + ".");
            }
        }
    }

    private Driver findDriverByBody(String bodyNo) {
        for (Driver d : drivers.values()) {
            if (d.getAssignedTricycleBodyNo().equalsIgnoreCase(bodyNo)) {
                return d;
            }
        }
        return null;
    }

    /** Full record for the Driver Record drawer: profile, expenses, and recent trips. */
    public synchronized Map<String, Object> driverRecord(String id) throws NotFoundException {
        Driver d = getDriver(id);
        Map<String, Object> out = new LinkedHashMap<>(d.toJson());
        List<ExpenseRecord> exp = new ArrayList<>(d.getExpenses());
        Collections.reverse(exp);
        out.put("expenses", exp);
        List<TripRecord> trips = new ArrayList<>();
        for (TripRecord t : logger.getAll()) {
            if (t.getBodyNumber().equalsIgnoreCase(d.getAssignedTricycleBodyNo())) {
                trips.add(t);
            }
        }
        Collections.reverse(trips);
        out.put("recentTrips", trips.subList(0, Math.min(25, trips.size())));
        out.put("queuePosition", queue.positionOf(d.getAssignedTricycleBodyNo()));
        return out;
    }

    // =====================================================================================
    // Queue
    // =====================================================================================

    public synchronized List<QueuedTricycle> getQueue() {
        return queue.toList();
    }

    public synchronized QueuedTricycle enqueue(String rawBodyNo) throws TerminalException {
        String bodyNo = Driver.normalizeBodyNumber(rawBodyNo);
        Driver d = findDriverByBody(bodyNo);
        if (d == null) {
            throw new NotFoundException("No registered driver has tricycle " + bodyNo + ".");
        }
        if (!d.getStatus().canQueue()) {
            throw new InvalidInputException(d.getName() + " is " + d.getStatus().getLabel()
                    + ". Set them to Active before adding " + bodyNo + " to the queue.");
        }
        if (queue.findByBodyNumber(bodyNo) != null) {
            throw new DuplicateEntryException(bodyNo + " is already in the queue (position #" + queue.positionOf(bodyNo) + ").");
        }
        QueuedTricycle t = new QueuedTricycle(d, settings.getDefaultMaxCapacity(), LocalDateTime.now());
        queue.enqueue(t);
        persist();
        return t;
    }

    public synchronized QueuedTricycle removeFromQueue(String rawBodyNo, boolean force) throws TerminalException {
        QueuedTricycle t = requireQueued(rawBodyNo);
        if (t.getCurrentPassengers() > 0 && !force) {
            throw new InvalidInputException(t.getBodyNumber() + " has " + t.getCurrentPassengers()
                    + " passenger(s) aboard. Dispatch it or clear the passengers first.");
        }
        queue.removeByBodyNumber(t.getBodyNumber());
        persist();
        return t;
    }

    public synchronized List<QueuedTricycle> moveUp(String rawBodyNo) throws TerminalException {
        QueuedTricycle t = requireQueued(rawBodyNo);
        queue.moveUp(t.getBodyNumber());
        persist();
        return queue.toList();
    }

    public synchronized List<QueuedTricycle> moveToFront(String rawBodyNo) throws TerminalException {
        QueuedTricycle t = requireQueued(rawBodyNo);
        queue.moveToFront(t.getBodyNumber());
        persist();
        return queue.toList();
    }

    public synchronized QueuedTricycle clearPassengers(String rawBodyNo) throws TerminalException {
        QueuedTricycle t = requireQueued(rawBodyNo);
        t.clearPassengers();
        persist();
        return t;
    }

    /** Special Trip Override: lock (or unlock) a unit as a private hire and move it to the front. */
    public synchronized QueuedTricycle setSpecialTrip(String rawBodyNo, RequestBody body) throws TerminalException {
        QueuedTricycle t = requireQueued(rawBodyNo);
        boolean enable = body.optBoolean("enabled", true);
        if (!enable) {
            t.disableSpecialTrip();
            persist();
            return t;
        }
        Destination dest = requireActiveDestination(body.getString("destinationId"));
        t.enableSpecialTrip(dest.getId(), dest.getName(), body.optInt("partySize", 1));
        queue.moveToFront(t.getBodyNumber());
        persist();
        return t;
    }

    private QueuedTricycle requireQueued(String rawBodyNo) throws TerminalException {
        String bodyNo = Driver.normalizeBodyNumber(rawBodyNo);
        if (queue.isEmpty()) {
            throw new EmptyQueueException("The queue is empty.");
        }
        QueuedTricycle t = queue.findByBodyNumber(bodyNo);
        if (t == null) {
            throw new NotFoundException(bodyNo + " is not in the queue.");
        }
        return t;
    }

    private Destination requireActiveDestination(String id) throws TerminalException {
        Destination d = requireDestination(id);
        if (!d.isActive()) {
            throw new InvalidInputException(d.getName() + " is currently deactivated.");
        }
        return d;
    }

    /**
     * Auto-route sorting: units that are already boarding (or locked as special trips) move ahead
     * of empty units. The sort is stable, so within each group the original FIFO order holds.
     */
    private void applyRouteSorting() {
        queue.sort(Comparator.comparingInt(TerminalDispatcher::sortRank));
    }

    private static int sortRank(QueuedTricycle t) {
        return switch (t.getQueueStatus()) {
            case SPECIAL_TRIP -> 0;
            case BOARDING -> 1;
            case IN_QUEUE -> 2;
        };
    }

    // =====================================================================================
    // Boarding
    // =====================================================================================

    /** Smart matching without changing anything: returns the receipt and seat options. */
    public synchronized BoardingPlan previewBoarding(RequestBody body) throws TerminalException {
        Destination dest = requireActiveDestination(body.getString("destinationId"));
        Passenger party = new Passenger("PAX-PREVIEW", body.optString("name", ""), dest.getName(),
                body.getInt("partySize"), body.optInt("discounted", 0));
        return plan(dest, party);
    }

    /**
     * Boards a party. mode = "auto" (only when the first eligible unit fits everyone),
     * "whole" (next unit with enough seats) or "split" (fill units in FIFO order).
     */
    public synchronized Map<String, Object> board(RequestBody body) throws TerminalException {
        Destination dest = requireActiveDestination(body.getString("destinationId"));
        Passenger party = new Passenger("PAX-" + System.currentTimeMillis(), body.optString("name", ""),
                dest.getName(), body.getInt("partySize"), body.optInt("discounted", 0));
        BoardingPlan plan = plan(dest, party);
        String mode = body.optString("mode", "auto").toLowerCase(Locale.ROOT);
        List<SeatAllocation> chosen;
        switch (mode) {
            case "whole" -> chosen = plan.getWholeOption();
            case "split" -> chosen = plan.getSplitOption() != null ? plan.getSplitOption() : plan.getWholeOption();
            case "auto" -> {
                if (plan.choiceRequired()) {
                    throw new CapacityExceededException("The party of " + party.getPartySize()
                            + " does not fit the next tricycle. Choose to split them or use the next empty unit.");
                }
                chosen = plan.getWholeOption() != null ? plan.getWholeOption() : plan.getSplitOption();
            }
            default -> throw new InvalidInputException("Boarding mode must be auto, whole or split.");
        }
        if (chosen == null) {
            throw new CapacityExceededException("No single tricycle has " + party.getPartySize()
                    + " free seats for " + dest.getName() + ". Split the party instead.");
        }
        int slice = 1;
        for (SeatAllocation a : chosen) {
            QueuedTricycle t = queue.findByBodyNumber(a.bodyNumber());
            String label = chosen.size() > 1 ? party.getName() + " (" + slice + "/" + chosen.size() + ")" : party.getName();
            t.board(new Passenger(party.getId() + "-" + slice, label, dest.getName(), a.seats(), a.discounted()),
                    dest.getId(), dest.getName());
            slice++;
        }
        if (settings.isAutoRouteSorting()) {
            applyRouteSorting();
        }
        persist();
        Map<String, Object> out = new LinkedHashMap<>(plan.toJson());
        out.put("boarded", chosen);
        out.put("mode", chosen == plan.getSplitOption() ? "split" : "whole");
        out.put("queue", queue.toList());
        return out;
    }

    private BoardingPlan plan(Destination dest, Passenger party) throws TerminalException {
        if (queue.isEmpty()) {
            throw new EmptyQueueException("No tricycles are in the queue. Add one before boarding passengers.");
        }
        List<QueuedTricycle> line = queue.toList();
        List<QueuedTricycle> candidates = new ArrayList<>();
        for (QueuedTricycle t : line) {
            if (!t.isSpecialTrip() && t.servesDestination(dest.getId()) && t.getRemainingSeats() > 0) {
                candidates.add(t);
            }
        }
        if (settings.isAutoRouteSorting()) {
            // seat-fill efficiency: fill units already heading there before opening an empty one
            candidates.sort(Comparator.comparingInt(t -> t.getDestinationId() == null ? 1 : 0));
        }
        int freeSeats = 0;
        for (QueuedTricycle t : candidates) {
            freeSeats += t.getRemainingSeats();
        }
        if (freeSeats < party.getPartySize()) {
            throw new CapacityExceededException("Not enough seats for " + dest.getName() + ": the party needs "
                    + party.getPartySize() + " but only " + freeSeats + " are free. Add more tricycles to the queue.");
        }

        RegularTrip quote = quote(dest, party.getRegularCount(), party.getDiscountCount());
        QueuedTricycle wholeUnit = null;
        for (QueuedTricycle t : candidates) {
            if (t.getRemainingSeats() >= party.getPartySize()) {
                wholeUnit = t;
                break;
            }
        }
        List<SeatAllocation> whole = null;
        if (wholeUnit != null) {
            whole = List.of(new SeatAllocation(wholeUnit.getBodyNumber(), wholeUnit.getDriverName(),
                    queue.positionOf(wholeUnit.getBodyNumber()), party.getRegularCount(), party.getDiscountCount(),
                    wholeUnit.getRemainingSeats() - party.getPartySize(), quote.calculateFare()));
        }
        List<SeatAllocation> split = null;
        String note;
        if (wholeUnit != null && wholeUnit == candidates.get(0)) {
            note = "Boards " + wholeUnit.getBodyNumber() + " (#" + queue.positionOf(wholeUnit.getBodyNumber()) + " in line).";
        } else {
            split = new ArrayList<>();
            int left = party.getPartySize();
            int discLeft = party.getDiscountCount();
            for (QueuedTricycle t : candidates) {
                if (left == 0) {
                    break;
                }
                int take = Math.min(left, t.getRemainingSeats());
                int disc = Math.min(discLeft, take);
                RegularTrip slice = quote(dest, take - disc, disc);
                split.add(new SeatAllocation(t.getBodyNumber(), t.getDriverName(), queue.positionOf(t.getBodyNumber()),
                        take - disc, disc, t.getRemainingSeats() - take, slice.calculateFare()));
                left -= take;
                discLeft -= disc;
            }
            QueuedTricycle first = candidates.get(0);
            note = "The party of " + party.getPartySize() + " is larger than the " + first.getRemainingSeats()
                    + " seat(s) left on " + first.getBodyNumber() + "."
                    + (wholeUnit != null ? " Split them, or send everyone on " + wholeUnit.getBodyNumber() + "." : " They will be split across units.");
        }
        return new BoardingPlan(dest.getId(), dest.getName(), party.getPartySize(), party.getDiscountCount(),
                quote.getFareBreakdown(), quote.calculateFare(), whole, split, note);
    }

    private RegularTrip quote(Destination dest, int regular, int discounted) throws InvalidInputException {
        return new RegularTrip("QUOTE", LocalDateTime.now(), "-", "-", dest.getName(), regular, discounted,
                dest.getBaseFare(), settings.getDiscountRate());
    }

    // =====================================================================================
    // Dispatch
    // =====================================================================================

    /** Dispatches the given unit, or the head of the line when bodyNo is blank. */
    public synchronized Map<String, Object> dispatch(String rawBodyNo) throws TerminalException {
        QueuedTricycle unit = (rawBodyNo == null || rawBodyNo.isBlank()) ? queue.peek() : requireQueued(rawBodyNo);
        if (unit.getCurrentPassengers() == 0) {
            throw new InvalidInputException(unit.getBodyNumber() + " has no passengers yet. Board a party first.");
        }
        Driver driver = findDriverByBody(unit.getBodyNumber());
        if (driver == null) {
            throw new NotFoundException("The driver for " + unit.getBodyNumber() + " is no longer registered.");
        }
        Destination dest = destinations.get(unit.getDestinationId());
        if (dest == null) {
            throw new NotFoundException("The route for " + unit.getBodyNumber() + " no longer exists. Clear and re-board it.");
        }
        LocalDateTime now = LocalDateTime.now();
        Trip trip = buildTrip(logger.nextTripId(now.toLocalDate()), now, unit, driver, dest);
        TripRecord record = trip.toRecord();
        driver.completeTrip(record.getTotalFare(), record.getTotalPax());
        try {
            logger.append(record);
        } catch (IOException ex) {
            throw new InvalidInputException("Trip saved but the log file could not be written: " + ex.getMessage());
        } finally {
            queue.removeByBodyNumber(unit.getBodyNumber());
            persist();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("trip", trip);
        out.put("record", record);
        out.put("driver", driver);
        return out;
    }

    /** The only place that decides which Trip subclass to create; everything after is polymorphic. */
    private Trip buildTrip(String id, LocalDateTime when, QueuedTricycle unit, Driver driver, Destination dest)
            throws InvalidInputException {
        if (unit.isSpecialTrip()) {
            return new SpecialTrip(id, when, unit.getBodyNumber(), driver.getName(), dest.getName(),
                    unit.getSpecialPartySize(), dest.getBaseFare(),
                    settings.getSpecialMode() == TerminalSettings.SpecialMode.FLAT,
                    settings.getSpecialMultiplier(), settings.getSpecialFlatRate());
        }
        return new RegularTrip(id, when, unit.getBodyNumber(), driver.getName(), dest.getName(),
                unit.getRegularPax(), unit.getDiscountedPax(), dest.getBaseFare(), settings.getDiscountRate());
    }

    /** Fare the dispatcher will collect if they dispatch this unit now. */
    private double projectedFare(QueuedTricycle unit) {
        Destination dest = unit.getDestinationId() == null ? null : destinations.get(unit.getDestinationId());
        Driver driver = findDriverByBody(unit.getBodyNumber());
        if (dest == null || driver == null || unit.getCurrentPassengers() == 0) {
            return 0;
        }
        try {
            return buildTrip("PREVIEW", LocalDateTime.now(), unit, driver, dest).calculateFare();
        } catch (InvalidInputException ex) {
            return 0;
        }
    }

    // =====================================================================================
    // Expenses
    // =====================================================================================

    public synchronized ExpenseRecord logExpense(RequestBody body) throws TerminalException {
        Driver d = getDriver(body.getString("driverId"));
        ExpenseRecord e = new ExpenseRecord(nextExpenseId(), d.getId(), ExpenseCategory.parse(body.getString("category")),
                body.getDouble("amount"), LocalDateTime.now(), body.optString("note", ""));
        d.recordExpense(e);
        try {
            logger.appendExpense(d, e);
        } catch (IOException ex) {
            System.err.println("[Dispatcher] Expense log write failed: " + ex.getMessage());
        }
        persist();
        return e;
    }

    public synchronized List<Map<String, Object>> listExpenses(String driverId, LocalDate from, LocalDate to) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Driver d : drivers.values()) {
            if (driverId != null && !driverId.isBlank() && !d.getId().equals(driverId)) {
                continue;
            }
            for (ExpenseRecord e : d.getExpenses()) {
                LocalDate day = e.getTimestamp().toLocalDate();
                if ((from != null && day.isBefore(from)) || (to != null && day.isAfter(to))) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>(e.toJson());
                row.put("driverName", d.getName());
                row.put("bodyNumber", d.getAssignedTricycleBodyNo());
                out.add(row);
            }
        }
        out.sort((a, b) -> String.valueOf(b.get("timestamp")).compareTo(String.valueOf(a.get("timestamp"))));
        return out;
    }

    private String nextExpenseId() {
        expenseSeq++;
        return String.format("EXP-%05d", expenseSeq);
    }

    // =====================================================================================
    // Dashboard, analytics, logs
    // =====================================================================================

    public synchronized Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        Map<String, Object> analytics = new AnalyticsService(logger.getAll(), drivers.values(),
                settings.getDefaultMaxCapacity()).summarize(today, today);
        @SuppressWarnings("unchecked")
        Map<String, Object> totals = (Map<String, Object>) analytics.get("totals");

        int active = 0;
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (DriverStatus s : DriverStatus.values()) {
            statusCounts.put(s.name(), 0);
        }
        for (Driver d : drivers.values()) {
            statusCounts.merge(d.getStatus().name(), 1, Integer::sum);
            if (d.getStatus() == DriverStatus.ACTIVE) {
                active++;
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("tricyclesInOperation", active);
        stats.put("totalDrivers", drivers.size());
        stats.put("inQueue", queue.size());
        stats.put("passengersToday", totals.get("passengers"));
        stats.put("tripsToday", totals.get("trips"));
        stats.put("grossToday", totals.get("gross"));
        stats.put("expensesToday", totals.get("expenses"));
        stats.put("netToday", totals.get("net"));
        stats.put("statusCounts", statusCounts);

        List<Map<String, Object>> line = new ArrayList<>();
        int pos = 1;
        for (QueuedTricycle t : queue.toList()) {
            Map<String, Object> row = new LinkedHashMap<>(t.toJson());
            row.put("position", pos++);
            row.put("projectedFare", projectedFare(t));
            line.add(row);
        }
        List<TripRecord> all = logger.getAll();
        List<TripRecord> recent = new ArrayList<>(all.subList(Math.max(0, all.size() - 6), all.size()));
        Collections.reverse(recent);

        List<Driver> waiting = new ArrayList<>();
        for (Driver d : drivers.values()) {
            if (d.getStatus().canQueue() && queue.findByBodyNumber(d.getAssignedTricycleBodyNo()) == null) {
                waiting.add(d);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("terminalName", settings.getTerminalName());
        out.put("stats", stats);
        out.put("nowBoarding", line.isEmpty() ? null : line.get(0));
        out.put("queue", line);
        out.put("recentTrips", recent);
        out.put("availableDrivers", waiting);
        out.put("destinations", listDestinations());
        out.put("settings", settings);
        return out;
    }

    public synchronized Map<String, Object> analytics(LocalDate from, LocalDate to) throws InvalidInputException {
        if (from == null || to == null) {
            throw new InvalidInputException("Both 'from' and 'to' dates are required.");
        }
        if (to.isBefore(from)) {
            throw new InvalidInputException("The end date must be on or after the start date.");
        }
        if (ChronoUnit.DAYS.between(from, to) > 92) {
            throw new InvalidInputException("Please choose a range of 93 days or fewer.");
        }
        return new AnalyticsService(logger.getAll(), drivers.values(), settings.getDefaultMaxCapacity()).summarize(from, to);
    }

    /** Filters the dispatch log. All parameters are optional. */
    public List<TripRecord> searchLogs(String q, LocalDate from, LocalDate to, String type, String destination, String bodyNo) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<TripRecord> out = new ArrayList<>();
        for (TripRecord r : logger.getAll()) {
            LocalDate day = r.getTimestamp().toLocalDate();
            if (from != null && day.isBefore(from)) {
                continue;
            }
            if (to != null && day.isAfter(to)) {
                continue;
            }
            if (type != null && !type.isBlank() && !r.getTripType().equalsIgnoreCase(type)) {
                continue;
            }
            if (destination != null && !destination.isBlank() && !r.getDestination().equalsIgnoreCase(destination)) {
                continue;
            }
            if (bodyNo != null && !bodyNo.isBlank() && !r.getBodyNumber().equalsIgnoreCase(bodyNo)) {
                continue;
            }
            if (!needle.isEmpty()) {
                String hay = (r.getTripId() + " " + r.getBodyNumber() + " " + r.getDriverName() + " "
                        + r.getDestination() + " " + r.getTripType()).toLowerCase(Locale.ROOT);
                if (!hay.contains(needle)) {
                    continue;
                }
            }
            out.add(r);
        }
        Collections.reverse(out);
        return out;
    }

    public String exportCsv(List<TripRecord> rows) {
        StringBuilder sb = new StringBuilder(TripRecord.CSV_HEADER).append("\r\n");
        List<TripRecord> ordered = new ArrayList<>(rows);
        Collections.reverse(ordered);
        for (TripRecord r : ordered) {
            sb.append(r.toCsvRow()).append("\r\n");
        }
        return sb.toString();
    }

    // =====================================================================================
    // Persistence
    // =====================================================================================

    /** Turned off while seeding so hundreds of demo rows do not each rewrite the state file. */
    public synchronized void setPersistenceEnabled(boolean enabled) {
        this.persistenceEnabled = enabled;
    }

    public synchronized void persist() {
        if (!persistenceEnabled) {
            return;
        }
        try {
            store.save(snapshot());
        } catch (IOException ex) {
            System.err.println("[Dispatcher] Could not save state: " + ex.getMessage());
        }
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("version", 1);
        s.put("savedAt", LocalDateTime.now().toString());
        s.put("driverSeq", driverSeq);
        s.put("expenseSeq", expenseSeq);
        s.put("settings", settings);
        s.put("destinations", new ArrayList<>(destinations.values()));
        List<Map<String, Object>> ds = new ArrayList<>();
        for (Driver d : drivers.values()) {
            Map<String, Object> m = d.toJson();
            m.put("expenses", new ArrayList<>(d.getExpenses()));
            ds.add(m);
        }
        s.put("drivers", ds);
        s.put("queue", queue.toList());
        return s;
    }

    /** Rebuilds the terminal from state.json. Bad rows are skipped and reported, never fatal. */
    @SuppressWarnings("unchecked")
    public synchronized void restore(Map<String, Object> s) {
        RequestBody root = new RequestBody(s);
        try {
            driverSeq = root.optInt("driverSeq", 0);
            expenseSeq = root.optInt("expenseSeq", 0);
        } catch (InvalidInputException ignored) {
            // fall back to zero and recount below
        }
        Object rawSettings = s.get("settings");
        if (rawSettings instanceof Map) {
            try {
                settings = new TerminalSettings().withUpdates(new RequestBody((Map<String, Object>) rawSettings));
            } catch (InvalidInputException ex) {
                System.err.println("[Restore] Settings invalid, using defaults: " + ex.getMessage());
            }
        }
        for (Object o : listOf(s.get("destinations"))) {
            try {
                RequestBody b = new RequestBody((Map<String, Object>) o);
                Destination d = new Destination(b.getString("id"), b.getString("name"), b.getDouble("baseFare"),
                        b.optBoolean("active", true));
                destinations.put(d.getId(), d);
            } catch (TerminalException | ClassCastException ex) {
                System.err.println("[Restore] Skipped a destination: " + ex.getMessage());
            }
        }
        for (Object o : listOf(s.get("drivers"))) {
            try {
                RequestBody b = new RequestBody((Map<String, Object>) o);
                Driver d = new Driver(b.getString("id"), b.getString("name"), b.optString("contactNumber", ""),
                        b.getString("licenseNumber"), b.getString("bodyNumber"));
                d.setStatus(DriverStatus.parse(b.optString("status", "ACTIVE")));
                d.restoreTotals(b.optDouble("grossEarnings", 0), b.optInt("tripsCompleted", 0), b.optInt("passengersServed", 0));
                for (Object eo : listOf(((Map<String, Object>) o).get("expenses"))) {
                    RequestBody eb = new RequestBody((Map<String, Object>) eo);
                    d.recordExpense(new ExpenseRecord(eb.getString("id"), d.getId(),
                            ExpenseCategory.parse(eb.getString("category")), eb.getDouble("amount"),
                            LocalDateTime.parse(eb.getString("timestamp")), eb.optString("note", "")));
                }
                drivers.put(d.getId(), d);
                driverSeq = Math.max(driverSeq, parseSeq(d.getId()));
            } catch (TerminalException | RuntimeException ex) {
                System.err.println("[Restore] Skipped a driver: " + ex.getMessage());
            }
        }
        for (Object o : listOf(s.get("queue"))) {
            try {
                RequestBody b = new RequestBody((Map<String, Object>) o);
                Driver d = findDriverByBody(b.getString("bodyNumber"));
                if (d == null || !d.getStatus().canQueue() || queue.findByBodyNumber(d.getAssignedTricycleBodyNo()) != null) {
                    continue;
                }
                QueuedTricycle t = new QueuedTricycle(d, b.optInt("maxCapacity", settings.getDefaultMaxCapacity()),
                        LocalDateTime.parse(b.getString("joinedAt")));
                String destId = b.optString("destinationId", null);
                Destination dest = destId == null ? null : destinations.get(destId);
                if (dest != null) {
                    t.restoreSeats(dest.getId(), dest.getName(), b.optInt("regularPax", 0), b.optInt("discountedPax", 0),
                            b.optBoolean("specialTrip", false), b.optInt("specialPartySize", 0));
                }
                queue.enqueue(t);
            } catch (TerminalException | RuntimeException ex) {
                System.err.println("[Restore] Skipped a queue entry: " + ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listOf(Object o) {
        return o instanceof List ? (List<Object>) o : Collections.emptyList();
    }

    private static int parseSeq(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    // ------------------------------------------------------------ seeding hooks

    /** Used by {@link SeedData} to write demo history through the same logger as real dispatches. */
    synchronized void seedTrip(Trip trip, Driver driver) throws TerminalException, IOException {
        TripRecord record = trip.toRecord();
        driver.completeTrip(record.getTotalFare(), record.getTotalPax());
        logger.append(record);
    }

    synchronized void seedExpense(Driver driver, ExpenseCategory cat, double amount, LocalDateTime when, String note)
            throws TerminalException {
        driver.recordExpense(new ExpenseRecord(nextExpenseId(), driver.getId(), cat, amount, when, note));
    }

    synchronized TripLogger getLogger() {
        return logger;
    }

    synchronized Collection<Destination> destinationValues() {
        return destinations.values();
    }
}
