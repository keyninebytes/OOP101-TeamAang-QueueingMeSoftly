// src/com/toda/SelfTest.java
package com.toda;

import com.toda.exception.CapacityExceededException;
import com.toda.exception.DuplicateEntryException;
import com.toda.exception.EmptyQueueException;
import com.toda.exception.InvalidInputException;
import com.toda.exception.TerminalException;
import com.toda.model.Driver;
import com.toda.model.Passenger;
import com.toda.model.QueuedTricycle;
import com.toda.model.RegularTrip;
import com.toda.model.SpecialTrip;
import com.toda.model.Trip;
import com.toda.structure.CustomQueue;
import com.toda.structure.TricycleQueue;
import com.toda.service.DataStore;
import com.toda.service.SeedData;
import com.toda.service.TerminalDispatcher;
import com.toda.service.TripLogger;
import com.toda.util.RequestBody;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dependency-free self test (no JUnit needed). Run with:
 *   java -cp out com.toda.SelfTest
 * It exercises the custom queue, fare polymorphism, encapsulation guards and the dispatcher's
 * edge cases (empty queue, capacity overflow, negative input, duplicates).
 */
public final class SelfTest {
    private static int passed;
    private static int failed;

    private SelfTest() {
    }

    public static void main(String[] args) throws Exception {
        queueTests();
        fareTests();
        modelGuardTests();
        dispatcherTests();
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------ CustomQueue

    private static void queueTests() throws Exception {
        CustomQueue<String> q = new CustomQueue<>();
        expect("dequeue on empty queue throws EmptyQueueException", EmptyQueueException.class, q::dequeue);
        expect("peek on empty queue throws EmptyQueueException", EmptyQueueException.class, q::peek);
        q.enqueue("A");
        q.enqueue("B");
        q.enqueue("C");
        q.enqueue("D");
        check("FIFO peek returns first in", "A".equals(q.peek()));
        check("remove from middle keeps order", "C".equals(q.remove(s -> s.equals("C"))) && q.toList().equals(List.of("A", "B", "D")));
        q.moveToFront(s -> s.equals("D"));
        check("moveToFront puts item at head", q.toList().equals(List.of("D", "A", "B")));
        q.moveUp(s -> s.equals("B"));
        check("moveUp swaps with the one ahead", q.toList().equals(List.of("D", "B", "A")));
        check("dequeue returns head", "D".equals(q.dequeue()) && q.size() == 2);
        q.enqueue("E");
        q.sort((a, b) -> Integer.compare(a.equals("E") ? 0 : 1, b.equals("E") ? 0 : 1));
        check("stable sort keeps FIFO inside groups", q.toList().equals(List.of("E", "B", "A")));
        boolean immutable;
        try {
            q.toList().add("X");
            immutable = false;
        } catch (UnsupportedOperationException ex) {
            immutable = true;
        }
        check("toList snapshot cannot be mutated", immutable);
        while (!q.isEmpty()) {
            q.dequeue();
        }
        check("queue drains to empty", q.size() == 0 && q.toList().isEmpty());
    }

    // ------------------------------------------------------------ Trips (polymorphism)

    private static void fareTests() throws Exception {
        Trip regular = new RegularTrip("T1", LocalDateTime.now(), "T-001", "Juan", "Poblacion", 2, 1, 15, 0.20);
        Trip special = new SpecialTrip("T2", LocalDateTime.now(), "T-002", "Ramon", "Alangilan", 2, 30, false, 4, 80);
        Trip flat = new SpecialTrip("T3", LocalDateTime.now(), "T-003", "Mario", "Alangilan", 2, 30, true, 4, 80);
        check("2 regular + 1 student at PHP 15 = PHP 42.00", regular.calculateFare() == 42.00);
        check("special multiplier PHP 30 x 4 = PHP 120.00", special.calculateFare() == 120.00);
        check("special flat rate = PHP 80.00", flat.calculateFare() == 80.00);
        double sum = 0;
        for (Trip t : List.of(regular, special, flat)) {
            sum += t.calculateFare(); // same call, three different pricing rules
        }
        check("polymorphic total is PHP 242.00", sum == 242.00);
        double lines = regular.getFareBreakdown().stream().mapToDouble(l -> l.amount()).sum();
        check("receipt lines add up to the fare", lines == regular.calculateFare());
        check("trip types are labelled", "REGULAR".equals(regular.getTripType()) && "SPECIAL".equals(special.getTripType()));
    }

    // ------------------------------------------------------------ Encapsulation guards

    private static void modelGuardTests() throws Exception {
        Driver d = new Driver("DRV-1", "Juan Dela Cruz", "0917 123 4567", "D04-18-100231", "t1");
        check("body number normalised to T-001", "T-001".equals(d.getAssignedTricycleBodyNo()));
        expect("negative earnings rejected", InvalidInputException.class, () -> d.addEarnings(-5));
        expect("zero expense rejected", InvalidInputException.class, () -> d.recordExpense("FUEL", 0));
        expect("empty body number rejected", InvalidInputException.class, () -> d.setAssignedTricycleBodyNo("  "));
        d.addEarnings(500);
        d.recordExpense("Gasoline/Fuel", 120);
        d.recordExpense("BOUNDARY", 150);
        check("net profit = gross - expenses (PHP 230.00)", d.getNetProfit() == 230.00);
        boolean readOnly;
        try {
            d.getExpenses().clear();
            readOnly = false;
        } catch (UnsupportedOperationException ex) {
            readOnly = true;
        }
        check("expense list is read-only from outside", readOnly && d.getExpenses().size() == 2);
        expect("discounted count above party size rejected", InvalidInputException.class,
                () -> new Passenger("P", "", "Poblacion", 2, 3));
        expect("negative party size rejected", InvalidInputException.class,
                () -> new Passenger("P", "", "Poblacion", -1, 0));
        QueuedTricycle t = new QueuedTricycle(d, 4, LocalDateTime.now());
        t.board(new Passenger("P1", "", "Poblacion", 3, 0), "poblacion", "Poblacion");
        expect("boarding past max capacity throws CapacityExceededException", CapacityExceededException.class,
                () -> t.board(new Passenger("P2", "", "Poblacion", 2, 0), "poblacion", "Poblacion"));
        expect("boarding a different destination rejected", InvalidInputException.class,
                () -> t.board(new Passenger("P3", "", "SM City", 1, 0), "sm-city", "SM City"));
        TricycleQueue tq = new TricycleQueue();
        tq.enqueue(t);
        check("TricycleQueue finds by body number", tq.findByBodyNumber("t-001") == t);
    }

    // ------------------------------------------------------------ Dispatcher edge cases

    private static void dispatcherTests() throws Exception {
        Path tmp = Files.createTempDirectory("qms-selftest");
        TerminalDispatcher terminal = new TerminalDispatcher(new DataStore(tmp), new TripLogger(tmp));
        expect("dispatch on empty queue throws EmptyQueueException", EmptyQueueException.class, () -> terminal.dispatch(""));
        SeedData.seed(terminal, false);
        check("seed created 8 drivers and 5 routes", terminal.listDrivers().size() == 8 && terminal.listDestinations().size() == 5);
        check("seed queued 6 active units", terminal.getQueue().size() == 6);
        expect("duplicate body number rejected", DuplicateEntryException.class, () -> terminal.registerDriver(body(
                "name", "Copy Cat", "licenseNumber", "D99-00-000001", "bodyNumber", "T-001")));
        expect("duplicate license rejected", DuplicateEntryException.class, () -> terminal.registerDriver(body(
                "name", "Copy Cat", "licenseNumber", "D04-18-100231", "bodyNumber", "T-050")));
        expect("same unit cannot queue twice", DuplicateEntryException.class, () -> terminal.enqueue("T-002"));
        expect("on-break driver cannot queue", InvalidInputException.class, () -> terminal.enqueue("T-007"));
        expect("negative expense rejected", InvalidInputException.class, () -> terminal.logExpense(body(
                "driverId", "DRV-0001", "category", "FUEL", "amount", -50.0)));
        expect("party larger than all free seats rejected", CapacityExceededException.class, () -> terminal.board(body(
                "destinationId", "sm-city", "partySize", 40.0, "discounted", 0.0)));
        expect("auto mode asks before splitting", CapacityExceededException.class, () -> terminal.board(body(
                "destinationId", "poblacion", "partySize", 3.0, "discounted", 0.0)));
        Map<String, Object> split = terminal.board(body("destinationId", "poblacion", "partySize", 3.0, "discounted", 1.0, "mode", "split"));
        check("split boarding fills T-001 then T-002", "split".equals(split.get("mode"))
                && terminal.getQueue().get(0).isFull() && terminal.getQueue().get(1).getCurrentPassengers() == 1);
        expect("empty unit cannot be dispatched", InvalidInputException.class, () -> terminal.dispatch("T-003"));
        Map<String, Object> out = terminal.dispatch("");
        check("dispatch removes head and books the fare", terminal.getQueue().size() == 5
                && terminal.getDriver("DRV-0001").getGrossEarnings() > 0 && out.get("record") != null);
        check("dispatch wrote the CSV log", Files.list(tmp.resolve("logs")).anyMatch(p -> p.getFileName().toString().startsWith("dispatch_logs_20")));
        expect("going on break with passengers aboard is blocked", InvalidInputException.class,
                () -> terminal.setDriverStatus("DRV-0002", "ON_BREAK"));
        terminal.setDriverStatus("DRV-0003", "MAINTENANCE");
        check("maintenance pulls unit from queue", terminal.getQueue().stream().noneMatch(q -> q.getBodyNumber().equals("T-003")));
        terminal.setSpecialTrip("T-005", body("enabled", true, "destinationId", "alangilan", "partySize", 2.0));
        check("special trip moves unit to the front", terminal.getQueue().get(0).getBodyNumber().equals("T-005"));
        Map<String, Object> sp = terminal.dispatch("");
        check("special trip is billed PHP 30 x 4 = PHP 120.00", ((com.toda.model.TripRecord) sp.get("record")).getTotalFare() == 120.00);

        // persistence round trip
        TerminalDispatcher reloaded = new TerminalDispatcher(new DataStore(tmp), new TripLogger(tmp));
        reloaded.restore(new DataStore(tmp).load());
        check("state survives a restart", reloaded.listDrivers().size() == 8
                && reloaded.getQueue().size() == terminal.getQueue().size()
                && reloaded.getDriver("DRV-0001").getGrossEarnings() == terminal.getDriver("DRV-0001").getGrossEarnings());
    }

    // ------------------------------------------------------------ helpers

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private static void expect(String name, Class<? extends TerminalException> type, Action action) {
        try {
            action.run();
            fail(name + " (no exception)");
        } catch (Exception ex) {
            if (type.isInstance(ex)) {
                pass(name);
            } else {
                fail(name + " (got " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
            }
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            pass(name);
        } else {
            fail(name);
        }
    }

    private static void pass(String name) {
        passed++;
        System.out.println("  PASS  " + name);
    }

    private static void fail(String name) {
        failed++;
        System.out.println("  FAIL  " + name);
    }

    private static RequestBody body(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return new RequestBody(m);
    }
}
