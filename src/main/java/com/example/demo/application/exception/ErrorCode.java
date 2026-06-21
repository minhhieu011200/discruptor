package com.example.demo.application.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // ── General (ec = HTTP status code) ──────────────────
    INTERNAL_SERVER_ERROR(500, "Internal server error"),
    BAD_REQUEST(400, "Bad request"),
    NOT_FOUND(404, "Resource not found"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isBusiness() {
        return code >= 1000;
    }
}
