// src/com/toda/service/TripLogger.java
package com.toda.service;

import com.toda.model.Driver;
import com.toda.model.ExpenseRecord;
import com.toda.model.TripRecord;
import com.toda.util.Csv;
import com.toda.util.Money;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Automated dispatch logger. Every completed trip is appended to
 * {@code logs/dispatch_logs_YYYY-MM-DD.csv} and the running {@code logs/dispatch_logs.txt}.
 * Methods are synchronized so two dispatchers clicking at once never interleave a row.
 * On start-up the CSV files are read back with a BufferedReader, so the CSVs are the source of truth.
 */
public class TripLogger {
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String EXPENSE_HEADER = "ExpenseID,Timestamp,DriverID,BodyNo,DriverName,Category,Amount,Note";

    private final Path logDir;
    private final List<TripRecord> records = new ArrayList<>();

    public TripLogger(Path dataDir) {
        this.logDir = dataDir.resolve("logs");
        try {
            Files.createDirectories(logDir);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot create log folder " + logDir, ex);
        }
        loadExisting();
    }

    /** Appends one dispatch to both the dated CSV and the TXT journal. */
    public synchronized void append(TripRecord record) throws IOException {
        Path csv = logDir.resolve("dispatch_logs_" + record.getTimestamp().toLocalDate().format(FILE_DATE) + ".csv");
        boolean fresh = Files.notExists(csv);
        try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (fresh) {
                w.write(TripRecord.CSV_HEADER);
                w.newLine();
            }
            w.write(record.toCsvRow());
            w.newLine();
        }
        try (BufferedWriter w = Files.newBufferedWriter(logDir.resolve("dispatch_logs.txt"), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(record.toTxtLine());
            w.newLine();
        }
        records.add(record);
    }

    /** Appends an expense to {@code logs/expense_logs.csv} for the paper trail. */
    public synchronized void appendExpense(Driver driver, ExpenseRecord e) throws IOException {
        Path csv = logDir.resolve("expense_logs.csv");
        boolean fresh = Files.notExists(csv);
        try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (fresh) {
                w.write(EXPENSE_HEADER);
                w.newLine();
            }
            w.write(Csv.join(e.getId(), e.getTimestamp().withNano(0).toString().replace('T', ' '), driver.getId(),
                    driver.getAssignedTricycleBodyNo(), driver.getName(), e.getCategory().getLabel(),
                    String.format("%.2f", e.getAmount()), e.getNote()));
            w.newLine();
        }
        try (BufferedWriter w = Files.newBufferedWriter(logDir.resolve("dispatch_logs.txt"), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(String.format("[%s] EXPENSE %s | %s (%s) | %s | %s",
                    e.getTimestamp().withNano(0).toString().replace('T', ' '), e.getId(),
                    driver.getAssignedTricycleBodyNo(), driver.getName(), e.getCategory().getLabel(),
                    Money.format(e.getAmount())));
            w.newLine();
        }
    }

    /** Snapshot of every trip, oldest first. */
    public synchronized List<TripRecord> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    public synchronized boolean isEmpty() {
        return records.isEmpty();
    }

    /** Next ID for the day, e.g. TRP-20261123-0007. */
    public synchronized String nextTripId(LocalDate date) {
        String prefix = "TRP-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        int max = 0;
        for (TripRecord r : records) {
            if (r.getTripId().startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(r.getTripId().substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // a hand-edited ID; skip it
                }
            }
        }
        return prefix + String.format("%04d", max + 1);
    }

    public Path getLogDir() {
        return logDir;
    }

    private void loadExisting() {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(logDir, "dispatch_logs_*.csv")) {
            for (Path p : ds) {
                files.add(p);
            }
        } catch (IOException ex) {
            System.err.println("[TripLogger] Could not list logs: " + ex.getMessage());
            return;
        }
        Collections.sort(files);
        int skipped = 0;
        for (Path file : files) {
            try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("TripID")) {
                        continue;
                    }
                    TripRecord rec = TripRecord.fromCsv(line);
                    if (rec == null) {
                        skipped++;
                    } else {
                        records.add(rec);
                    }
                }
            } catch (IOException ex) {
                System.err.println("[TripLogger] Could not read " + file + ": " + ex.getMessage());
            }
        }
        records.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        if (skipped > 0) {
            System.err.println("[TripLogger] Skipped " + skipped + " malformed log row(s).");
        }
    }
}
