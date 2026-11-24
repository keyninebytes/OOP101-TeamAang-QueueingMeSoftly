// src/com/toda/Main.java
package com.toda;

import com.sun.net.httpserver.HttpServer;
import com.toda.controller.ApiController;
import com.toda.controller.StaticFileHandler;
import com.toda.service.DataStore;
import com.toda.service.SeedData;
import com.toda.service.TerminalDispatcher;
import com.toda.service.TripLogger;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Entry point. Wires the layers together (store → logger → dispatcher → controller → server)
 * and starts the local HTTP server.
 *
 * Usage: java -cp out com.toda.Main [--port 8080] [--data data] [--web web] [--no-demo-history]
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        Path dataDir = Paths.get("data");
        Path webDir = Paths.get("web");
        boolean demoHistory = true;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--data" -> dataDir = Paths.get(args[++i]);
                case "--web" -> webDir = Paths.get(args[++i]);
                case "--no-demo-history" -> demoHistory = false;
                default -> {
                    System.err.println("Unknown option: " + args[i]);
                    System.exit(2);
                }
            }
        }
        if (!Files.isRegularFile(webDir.resolve("index.html"))) {
            System.err.println("Cannot find " + webDir.toAbsolutePath().resolve("index.html")
                    + ". Run from the project folder or pass --web <folder>.");
            System.exit(1);
        }

        DataStore store = new DataStore(dataDir);
        TripLogger logger = new TripLogger(dataDir);
        TerminalDispatcher terminal = new TerminalDispatcher(store, logger);
        Map<String, Object> saved = store.load();
        if (saved == null) {
            System.out.println("First run: loading Batangas demo routes and drivers T-001 to T-008...");
            SeedData.seed(terminal, demoHistory);
        } else {
            terminal.restore(saved);
        }

        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException ex) {
            System.err.println("Port " + port + " is already in use. Try: --port 8081");
            System.exit(1);
            return;
        }
        server.createContext("/api", new ApiController(terminal).buildRouter());
        server.createContext("/", new StaticFileHandler(webDir));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println();
        System.out.println("  Queueing Me Softly is running");
        System.out.println("  Terminal : " + terminal.getSettings().getTerminalName());
        System.out.println("  Open     : http://localhost:" + port);
        System.out.println("  Data     : " + dataDir.toAbsolutePath());
        System.out.println("  Logs     : " + logger.getLogDir().toAbsolutePath());
        System.out.println("  Stop     : Ctrl+C");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            terminal.persist();
            server.stop(0);
        }));
    }
}
