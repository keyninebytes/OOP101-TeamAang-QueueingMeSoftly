// src/com/toda/controller/ApiResponse.java
package com.toda.controller;

import com.toda.util.Json;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** An explicit response, for anything that is not a plain 200 JSON body (201s, CSV downloads, errors). */
public class ApiResponse {
    private final int status;
    private final String contentType;
    private final byte[] body;
    private final Map<String, String> headers = new LinkedHashMap<>();

    public ApiResponse(int status, String contentType, byte[] body) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
    }

    public static ApiResponse json(int status, Object value) {
        return new ApiResponse(status, "application/json; charset=utf-8", Json.stringify(value).getBytes(StandardCharsets.UTF_8));
    }

    public static ApiResponse error(int status, String code, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", code);
        err.put("message", message);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("ok", false);
        wrapper.put("error", err);
        return json(status, wrapper);
    }

    public static ApiResponse download(String filename, String contentType, String text) {
        ApiResponse r = new ApiResponse(200, contentType, text.getBytes(StandardCharsets.UTF_8));
        r.headers.put("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        return r;
    }

    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
