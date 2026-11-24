// src/com/toda/controller/ApiRequest.java
package com.toda.controller;

import com.toda.exception.InvalidInputException;
import com.toda.util.RequestBody;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** What a route handler sees: path variables, query string and a lazily parsed JSON body. */
public class ApiRequest {
    private final String method;
    private final Map<String, String> pathParams;
    private final Map<String, String> query;
    private final String rawBody;
    private RequestBody body;

    public ApiRequest(String method, Map<String, String> pathParams, String rawQuery, String rawBody) {
        this.method = method;
        this.pathParams = Collections.unmodifiableMap(pathParams);
        this.query = parseQuery(rawQuery);
        this.rawBody = rawBody;
    }

    public String getMethod() {
        return method;
    }

    public String path(String name) {
        return pathParams.get(name);
    }

    public String query(String name) {
        return query.get(name);
    }

    /** Parses an optional yyyy-MM-dd query parameter. */
    public LocalDate queryDate(String name) throws InvalidInputException {
        String v = query.get(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException ex) {
            throw new InvalidInputException("'" + name + "' must be a date like 2026-11-23.");
        }
    }

    public RequestBody body() throws InvalidInputException {
        if (body == null) {
            body = new RequestBody(rawBody);
        }
        return body;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            try {
                out.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) {
                // malformed escape: drop the parameter
            }
        }
        return out;
    }
}
