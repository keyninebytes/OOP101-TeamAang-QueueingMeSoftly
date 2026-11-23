// src/com/toda/service/DataStore.java
package com.toda.service;

import com.toda.util.Json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Saves the terminal state (settings, routes, roster, expenses, live queue) to
 * {@code data/state.json}. Writes go to a temp file first and are then moved into place, so a
 * crash mid-write never leaves a half-written file behind.
 */
public class DataStore {
    private final Path stateFile;

    public DataStore(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        this.stateFile = dataDir.resolve("state.json");
    }

    public synchronized void save(Map<String, Object> state) throws IOException {
        Path tmp = stateFile.resolveSibling("state.json.tmp");
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(Json.stringify(state));
        }
        try {
            Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(tmp, stateFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Returns the saved state, or null when there is none or it is unreadable (it is then backed up). */
    @SuppressWarnings("unchecked")
    public synchronized Map<String, Object> load() {
        if (Files.notExists(stateFile)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            Object parsed = Json.parse(sb.toString());
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            }
            throw new IllegalArgumentException("state.json is not an object");
        } catch (IOException | RuntimeException ex) {
            backupCorrupt(ex);
            return null;
        }
    }

    public void backupCorrupt(Exception cause) {
        try {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path backup = stateFile.resolveSibling("state.corrupt-" + stamp + ".json");
            Files.move(stateFile, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[DataStore] state.json could not be read (" + cause.getMessage()
                    + "). Moved it to " + backup.getFileName() + " and starting fresh.");
        } catch (IOException ex) {
            System.err.println("[DataStore] Could not back up unreadable state: " + ex.getMessage());
        }
    }
}
