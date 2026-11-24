// src/com/toda/controller/ApiController.java
package com.toda.controller;

import com.toda.exception.InvalidInputException;
import com.toda.model.TripRecord;
import com.toda.service.TerminalDispatcher;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps every REST endpoint to one TerminalDispatcher call. Controllers stay thin: no business
 * rules live here, only request parsing and response shaping.
 */
public class ApiController {
    private final TerminalDispatcher terminal;

    public ApiController(TerminalDispatcher terminal) {
        this.terminal = terminal;
    }

    public Router buildRouter() {
        Router r = new Router();

        // ---- dashboard & queue
        r.get("/api/dashboard", req -> terminal.dashboard());
        r.get("/api/queue", req -> terminal.getQueue());
        r.post("/api/queue", req -> ApiResponse.json(201, terminal.enqueue(req.body().getString("bodyNumber"))));
        r.delete("/api/queue/{bodyNo}", req -> terminal.removeFromQueue(req.path("bodyNo"), "true".equals(req.query("force"))));
        r.post("/api/queue/{bodyNo}/move-up", req -> terminal.moveUp(req.path("bodyNo")));
        r.post("/api/queue/{bodyNo}/move-front", req -> terminal.moveToFront(req.path("bodyNo")));
        r.post("/api/queue/{bodyNo}/clear", req -> terminal.clearPassengers(req.path("bodyNo")));
        r.post("/api/queue/{bodyNo}/special", req -> terminal.setSpecialTrip(req.path("bodyNo"), req.body()));

        // ---- boarding & dispatch
        r.post("/api/board/preview", req -> terminal.previewBoarding(req.body()));
        r.post("/api/board", req -> terminal.board(req.body()));
        r.post("/api/dispatch", req -> terminal.dispatch(req.body().optString("bodyNumber", "")));

        // ---- drivers
        r.get("/api/drivers", req -> terminal.listDrivers());
        r.post("/api/drivers", req -> ApiResponse.json(201, terminal.registerDriver(req.body())));
        r.get("/api/drivers/{id}", req -> terminal.driverRecord(req.path("id")));
        r.put("/api/drivers/{id}", req -> terminal.updateDriver(req.path("id"), req.body()));
        r.put("/api/drivers/{id}/status", req -> terminal.setDriverStatus(req.path("id"), req.body().getString("status")));
        r.delete("/api/drivers/{id}", req -> {
            terminal.deleteDriver(req.path("id"));
            return ok();
        });

        // ---- destinations
        r.get("/api/destinations", req -> terminal.listDestinations());
        r.post("/api/destinations", req -> ApiResponse.json(201, terminal.addDestination(req.body())));
        r.put("/api/destinations/{id}", req -> terminal.updateDestination(req.path("id"), req.body()));
        r.delete("/api/destinations/{id}", req -> {
            terminal.deleteDestination(req.path("id"));
            return ok();
        });

        // ---- expenses
        r.get("/api/expenses", req -> terminal.listExpenses(req.query("driverId"), req.queryDate("from"), req.queryDate("to")));
        r.post("/api/expenses", req -> ApiResponse.json(201, terminal.logExpense(req.body())));

        // ---- logs & analytics
        r.get("/api/logs", req -> {
            List<TripRecord> rows = searchLogs(req);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("count", rows.size());
            double total = 0;
            for (TripRecord t : rows) {
                total += t.getTotalFare();
            }
            out.put("totalFare", Math.round(total * 100) / 100.0);
            out.put("records", rows.size() > 500 ? rows.subList(0, 500) : rows);
            out.put("truncated", rows.size() > 500);
            return out;
        });
        r.get("/api/logs/export", req -> {
            String name = "dispatch_logs_export_" + LocalDate.now() + ".csv";
            return ApiResponse.download(name, "text/csv; charset=utf-8", terminal.exportCsv(searchLogs(req)));
        });
        r.get("/api/analytics", req -> {
            LocalDate today = LocalDate.now();
            String range = req.query("range") == null ? "7d" : req.query("range");
            return switch (range) {
                case "today" -> terminal.analytics(today, today);
                case "7d" -> terminal.analytics(today.minusDays(6), today);
                case "30d" -> terminal.analytics(today.minusDays(29), today);
                case "custom" -> terminal.analytics(req.queryDate("from"), req.queryDate("to"));
                default -> throw new InvalidInputException("range must be today, 7d, 30d or custom.");
            };
        });

        // ---- settings
        r.get("/api/settings", req -> terminal.getSettings());
        r.put("/api/settings", req -> terminal.updateSettings(req.body()));
        return r;
    }

    private List<TripRecord> searchLogs(ApiRequest req) throws InvalidInputException {
        LocalDate date = req.queryDate("date");
        LocalDate from = date != null ? date : req.queryDate("from");
        LocalDate to = date != null ? date : req.queryDate("to");
        return terminal.searchLogs(req.query("q"), from, to, req.query("type"), req.query("destination"), req.query("bodyNumber"));
    }

    private static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        return m;
    }
}
