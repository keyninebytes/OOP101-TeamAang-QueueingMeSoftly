// src/com/toda/service/AnalyticsService.java
package com.toda.service;

import com.toda.model.Driver;
import com.toda.model.ExpenseRecord;
import com.toda.model.TripRecord;
import com.toda.util.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only number crunching for the Trip Summary page. It never mutates drivers or logs;
 * it just folds trips and expenses into daily buckets and insight figures.
 */
public class AnalyticsService {
    private final List<TripRecord> trips;
    private final Collection<Driver> drivers;
    private final int seatCapacity;

    public AnalyticsService(List<TripRecord> trips, Collection<Driver> drivers, int seatCapacity) {
        this.trips = trips;
        this.drivers = drivers;
        this.seatCapacity = Math.max(1, seatCapacity);
    }

    public Map<String, Object> summarize(LocalDate from, LocalDate to) {
        Map<LocalDate, double[]> days = new TreeMap<>(); // gross, expenses, trips, passengers, seatsUsedRegular, regularTrips
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.put(d, new double[6]);
        }
        Map<String, double[]> byDestination = new TreeMap<>(); // trips, gross, passengers
        Map<String, double[]> byBody = new LinkedHashMap<>();   // trips, gross, passengers
        for (TripRecord t : trips) {
            LocalDate d = t.getTimestamp().toLocalDate();
            double[] b = days.get(d);
            if (b == null) {
                continue;
            }
            b[0] += t.getTotalFare();
            b[2] += 1;
            b[3] += t.getTotalPax();
            if ("REGULAR".equals(t.getTripType())) {
                b[4] += Math.min(t.getTotalPax(), seatCapacity);
                b[5] += 1;
            }
            double[] dest = byDestination.computeIfAbsent(t.getDestination(), k -> new double[3]);
            dest[0] += 1;
            dest[1] += t.getTotalFare();
            dest[2] += t.getTotalPax();
            double[] body = byBody.computeIfAbsent(t.getBodyNumber(), k -> new double[3]);
            body[0] += 1;
            body[1] += t.getTotalFare();
            body[2] += t.getTotalPax();
        }
        Map<String, Double> expenseByDriver = new LinkedHashMap<>();
        Map<String, Double> expenseByCategory = new LinkedHashMap<>();
        for (Driver dr : drivers) {
            for (ExpenseRecord e : dr.getExpenses()) {
                double[] b = days.get(e.getTimestamp().toLocalDate());
                if (b == null) {
                    continue;
                }
                b[1] += e.getAmount();
                expenseByDriver.merge(dr.getId(), e.getAmount(), Double::sum);
                expenseByCategory.merge(e.getCategory().getLabel(), e.getAmount(), Double::sum);
            }
        }

        List<Map<String, Object>> daily = new ArrayList<>();
        double gross = 0, expenses = 0, tripCount = 0, pax = 0, seatsUsed = 0, regularTrips = 0;
        LocalDate bestDay = null, busiestDay = null;
        double bestGross = -1, busiestTrips = -1;
        for (Map.Entry<LocalDate, double[]> e : days.entrySet()) {
            double[] b = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", e.getKey().toString());
            row.put("gross", Money.round(b[0]));
            row.put("expenses", Money.round(b[1]));
            row.put("net", Money.round(b[0] - b[1]));
            row.put("trips", (int) b[2]);
            row.put("passengers", (int) b[3]);
            daily.add(row);
            gross += b[0];
            expenses += b[1];
            tripCount += b[2];
            pax += b[3];
            seatsUsed += b[4];
            regularTrips += b[5];
            if (b[2] > 0 && b[0] > bestGross) {
                bestGross = b[0];
                bestDay = e.getKey();
            }
            if (b[2] > 0 && b[2] > busiestTrips) {
                busiestTrips = b[2];
                busiestDay = e.getKey();
            }
        }

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("gross", Money.round(gross));
        totals.put("expenses", Money.round(expenses));
        totals.put("net", Money.round(gross - expenses));
        totals.put("trips", (int) tripCount);
        totals.put("passengers", (int) pax);

        Map<String, Object> insights = new LinkedHashMap<>();
        insights.put("highestEarningDay", bestDay == null ? null : Map.of("date", bestDay.toString(), "gross", Money.round(bestGross)));
        insights.put("mostTripsDay", busiestDay == null ? null : Map.of("date", busiestDay.toString(), "trips", (int) busiestTrips));
        insights.put("averageFarePerTrip", tripCount == 0 ? 0.0 : Money.round(gross / tripCount));
        insights.put("dispatchEfficiency", regularTrips == 0 ? 0.0
                : Math.round(seatsUsed / (regularTrips * seatCapacity) * 1000) / 10.0);
        insights.put("averagePassengersPerTrip", tripCount == 0 ? 0.0 : Math.round(pax / tripCount * 10) / 10.0);

        List<Map<String, Object>> destRows = new ArrayList<>();
        for (Map.Entry<String, double[]> e : byDestination.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("destination", e.getKey());
            row.put("trips", (int) e.getValue()[0]);
            row.put("gross", Money.round(e.getValue()[1]));
            row.put("passengers", (int) e.getValue()[2]);
            destRows.add(row);
        }
        destRows.sort((a, b) -> Double.compare((double) b.get("gross"), (double) a.get("gross")));

        List<Map<String, Object>> driverRows = new ArrayList<>();
        for (Driver dr : drivers) {
            double[] b = byBody.getOrDefault(dr.getAssignedTricycleBodyNo(), new double[3]);
            double exp = expenseByDriver.getOrDefault(dr.getId(), 0.0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("driverId", dr.getId());
            row.put("name", dr.getName());
            row.put("bodyNumber", dr.getAssignedTricycleBodyNo());
            row.put("trips", (int) b[0]);
            row.put("passengers", (int) b[2]);
            row.put("gross", Money.round(b[1]));
            row.put("expenses", Money.round(exp));
            row.put("net", Money.round(b[1] - exp));
            driverRows.add(row);
        }
        driverRows.sort((a, b) -> Double.compare((double) b.get("net"), (double) a.get("net")));

        List<Map<String, Object>> categoryRows = new ArrayList<>();
        for (Map.Entry<String, Double> e : expenseByCategory.entrySet()) {
            categoryRows.add(Map.of("category", e.getKey(), "amount", Money.round(e.getValue())));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("totals", totals);
        out.put("daily", daily);
        out.put("insights", insights);
        out.put("destinations", destRows);
        out.put("drivers", driverRows);
        out.put("expenseCategories", categoryRows);
        return out;
    }
}
