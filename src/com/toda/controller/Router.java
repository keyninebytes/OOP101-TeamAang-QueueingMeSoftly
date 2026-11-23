// src/com/toda/controller/Router.java
package com.toda.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.toda.exception.TerminalException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal REST router on top of com.sun.net.httpserver. Patterns like {@code /api/drivers/{id}}
 * are matched segment by segment. This is also the single place where domain exceptions become
 * JSON error payloads: the exception itself says which HTTP status it maps to.
 */
public class Router implements HttpHandler {
    private static final int MAX_BODY_BYTES = 64 * 1024;

    private record Route(String method, String[] segments, RouteHandler handler) {
    }

    private final List<Route> routes = new ArrayList<>();

    public Router get(String pattern, RouteHandler h) {
        return add("GET", pattern, h);
    }

    public Router post(String pattern, RouteHandler h) {
        return add("POST", pattern, h);
    }

    public Router put(String pattern, RouteHandler h) {
        return add("PUT", pattern, h);
    }

    public Router delete(String pattern, RouteHandler h) {
        return add("DELETE", pattern, h);
    }

    private Router add(String method, String pattern, RouteHandler h) {
        routes.add(new Route(method, split(pattern), h));
        return this;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        ApiResponse response;
        try {
            response = route(ex);
        } catch (TerminalException e) {
            response = ApiResponse.error(e.getHttpStatus(), e.getErrorCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            response = ApiResponse.error(400, "BAD_REQUEST", e.getMessage() == null ? "Bad request." : e.getMessage());
        } catch (BodyTooLargeException e) {
            response = ApiResponse.error(413, "PAYLOAD_TOO_LARGE", "Request body is too large.");
        } catch (RuntimeException e) {
            System.err.println("[Router] Unexpected error on " + ex.getRequestMethod() + " " + ex.getRequestURI());
            e.printStackTrace();
            response = ApiResponse.error(500, "SERVER_ERROR", "Something went wrong on the terminal server. Please try again.");
        }
        send(ex, response);
    }

    private ApiResponse route(HttpExchange ex) throws TerminalException, IOException {
        String method = ex.getRequestMethod().toUpperCase();
        String[] actual = split(ex.getRequestURI().getRawPath());
        boolean pathMatched = false;
        for (Route r : routes) {
            Map<String, String> params = match(r.segments(), actual);
            if (params == null) {
                continue;
            }
            pathMatched = true;
            if (!r.method().equals(method)) {
                continue;
            }
            String body = ("POST".equals(method) || "PUT".equals(method)) ? readBody(ex.getRequestBody()) : "";
            ApiRequest req = new ApiRequest(method, params, ex.getRequestURI().getRawQuery(), body);
            Object result = r.handler().handle(req);
            if (result instanceof ApiResponse resp) {
                return resp;
            }
            return ApiResponse.json(200, result);
        }
        if (pathMatched) {
            return ApiResponse.error(405, "METHOD_NOT_ALLOWED", method + " is not supported here.");
        }
        return ApiResponse.error(404, "NOT_FOUND", "No API endpoint at " + ex.getRequestURI().getPath());
    }

    private static Map<String, String> match(String[] pattern, String[] actual) {
        if (pattern.length != actual.length) {
            return null;
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < pattern.length; i++) {
            String p = pattern[i];
            if (p.startsWith("{") && p.endsWith("}")) {
                params.put(p.substring(1, p.length() - 1), java.net.URLDecoder.decode(actual[i], StandardCharsets.UTF_8));
            } else if (!p.equals(actual[i])) {
                return null;
            }
        }
        return params;
    }

    private static String[] split(String path) {
        String p = path == null ? "" : path.replaceAll("^/+|/+$", "");
        return p.isEmpty() ? new String[0] : p.split("/+");
    }

    private static String readBody(InputStream in) throws IOException {
        byte[] data = in.readNBytes(MAX_BODY_BYTES + 1);
        if (data.length > MAX_BODY_BYTES) {
            throw new BodyTooLargeException();
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange ex, ApiResponse r) throws IOException {
        ex.getResponseHeaders().set("Content-Type", r.getContentType());
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        r.getHeaders().forEach((k, v) -> ex.getResponseHeaders().set(k, v));
        byte[] body = r.getBody();
        ex.sendResponseHeaders(r.getStatus(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
        ex.close();
    }

    private static final class BodyTooLargeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
