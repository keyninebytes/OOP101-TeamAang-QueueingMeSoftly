// src/com/toda/controller/RouteHandler.java
package com.toda.controller;

import com.toda.exception.TerminalException;

/** A single endpoint. Returning a plain object means "200 OK with this as JSON". */
@FunctionalInterface
public interface RouteHandler {
    Object handle(ApiRequest request) throws TerminalException;
}
