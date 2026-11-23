// src/com/toda/service/SeedData.java
package com.toda.service;

import com.toda.exception.TerminalException;
import com.toda.model.Destination;
import com.toda.model.Driver;
import com.toda.model.DriverStatus;
import com.toda.model.ExpenseCategory;
import com.toda.model.RegularTrip;
import com.toda.model.SpecialTrip;
import com.toda.model.TerminalSettings;
import com.toda.model.Trip;
import com.toda.util.RequestBody;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * First-run demo data: five Batangas routes, drivers T-001 to T-008, a live queue, and (unless
 * disabled) six days of sample dispatches and expenses so the charts have something to show.
 * Everything goes through the same service methods real users hit, so seeding obeys every rule.
 */
public final class SeedData {
    private SeedData() {
    }

    public static void seed(TerminalDispatcher terminal, boolean withHistory) throws TerminalException, IOException {
        terminal.setPersistenceEnabled(false);
        try {
            seedRoutes(terminal);
            List<Driver> roster = seedDrivers(terminal);
            if (withHistory && terminal.getLogger().isEmpty()) {
                seedHistory(terminal, roster);
            }
            seedQueue(terminal);
        } finally {
            terminal.setPersistenceEnabled(true);
            terminal.persist();
        }
    }

    private static void seedRoutes(TerminalDispatcher t) throws TerminalException {
        Object[][] routes = {
                {"Calumpang", 15.0}, {"SM City", 20.0}, {"Poblacion", 15.0},
                {"Batangas Pier", 25.0}, {"Alangilan", 30.0},
        };
        for (Object[] r : routes) {
            t.addDestination(body("name", r[0], "baseFare", r[1]));
        }
    }

    private static List<Driver> seedDrivers(TerminalDispatcher t) throws TerminalException {
        String[][] people = {
                {"Juan Dela Cruz", "0917 123 4501", "D04-18-100231", "T-001"},
                {"Ramon Magsino", "0918 223 4502", "D04-19-200452", "T-002"},
                {"Eduardo Panganiban", "0919 323 4503", "D04-17-300673", "T-003"},
                {"Mario Aguila", "0920 423 4504", "D04-20-400894", "T-004"},
                {"Rogelio Macalintal", "0921 523 4505", "D04-16-501015", "T-005"},
                {"Danilo Ilagan", "0922 623 4506", "D04-21-601236", "T-006"},
                {"Arnel Comia", "0923 723 4507", "D04-18-701457", "T-007"},
                {"Bernardo Hernandez", "0924 823 4508", "D04-15-801678", "T-008"},
        };
        List<Driver> roster = new ArrayList<>();
        for (String[] p : people) {
            roster.add(t.registerDriver(body("name", p[0], "contactNumber", p[1], "licenseNumber", p[2], "bodyNumber", p[3])));
        }
        return roster;
    }

    private static void seedQueue(TerminalDispatcher t) throws TerminalException {
        for (int i = 1; i <= 6; i++) {
            t.enqueue(String.format("T-%03d", i));
        }
        t.setDriverStatus(find(t, "T-007"), DriverStatus.ON_BREAK.name());
        t.setDriverStatus(find(t, "T-008"), DriverStatus.MAINTENANCE.name());
        t.board(body("destinationId", "poblacion", "partySize", 2.0, "discounted", 1.0, "name", "Santos family"));
    }

    private static String find(TerminalDispatcher t, String bodyNo) {
        for (Driver d : t.listDrivers()) {
            if (d.getAssignedTricycleBodyNo().equals(bodyNo)) {
                return d.getId();
            }
        }
        throw new IllegalStateException("Seed driver " + bodyNo + " missing");
    }

    private static void seedHistory(TerminalDispatcher t, List<Driver> roster) throws TerminalException, IOException {
        Random rng = new Random(2107);
        TerminalSettings s = t.getSettings();
        List<Destination> routes = new ArrayList<>(t.destinationValues());
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        for (int back = 6; back >= 0; back--) {
            LocalDate day = today.minusDays(back);
            LocalDateTime start = LocalDateTime.of(day, LocalTime.of(6, 0));
            LocalDateTime end = back == 0 ? now.minusMinutes(10) : LocalDateTime.of(day, LocalTime.of(19, 0));
            long minutes = java.time.Duration.between(start, end).toMinutes();
            int tripsThatDay = minutes <= 30 ? 0 : (int) Math.max(1, (26 + rng.nextInt(22)) * Math.min(1.0, minutes / 780.0));
            int seq = 0;
            for (int i = 0; i < tripsThatDay; i++) {
                Driver d = roster.get(rng.nextInt(6));
                Destination r = routes.get(rng.nextInt(routes.size()));
                LocalDateTime when = start.plusMinutes(i * (minutes / tripsThatDay) + rng.nextInt(5));
                String id = "TRP-" + day.toString().replace("-", "") + "-" + String.format("%04d", ++seq);
                Trip trip;
                if (rng.nextInt(12) == 0) {
                    trip = new SpecialTrip(id, when, d.getAssignedTricycleBodyNo(), d.getName(), r.getName(),
                            1 + rng.nextInt(3), r.getBaseFare(), false, s.getSpecialMultiplier(), s.getSpecialFlatRate());
                } else {
                    int pax = 2 + rng.nextInt(3);
                    int disc = rng.nextInt(Math.min(pax, 2) + 1);
                    trip = new RegularTrip(id, when, d.getAssignedTricycleBodyNo(), d.getName(), r.getName(),
                            pax - disc, disc, r.getBaseFare(), s.getDiscountRate());
                }
                t.seedTrip(trip, d);
            }
            for (int i = 0; i < 6; i++) {
                Driver d = roster.get(i);
                LocalDateTime morning = LocalDateTime.of(day, LocalTime.of(5, 30 + i));
                if (morning.isAfter(now)) {
                    continue;
                }
                t.seedExpense(d, ExpenseCategory.FUEL, 120 + rng.nextInt(5) * 10, morning, "Morning refill");
                t.seedExpense(d, ExpenseCategory.BOUNDARY, 150, morning.plusMinutes(5), "Daily boundary");
                t.seedExpense(d, ExpenseCategory.TERMINAL, 10, morning.plusMinutes(6), "Terminal fee");
                if (back > 0 && rng.nextBoolean()) {
                    t.seedExpense(d, ExpenseCategory.MEALS, 60 + rng.nextInt(4) * 10, morning.plusHours(6), "Lunch");
                }
            }
        }
    }

    private static RequestBody body(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return new RequestBody(m);
    }
}
