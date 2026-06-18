package com.example.demo.presentation.exception;

import com.example.demo.application.dto.BaseResponseDTO;
import com.example.demo.application.exception.AppException;
import com.example.demo.application.exception.ErrorCode;
import com.example.demo.application.exception.ErrorMessageRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMessageRegistry errorMessageRegistry;

    // ── 1. Business exception → HTTP 200 ─────────────────────────────────────
    @ExceptionHandler(AppException.class)
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = errorMessageRegistry.getMessage(errorCode); // từ Redis, fallback enum
        log.warn("[EX] AppException: ec={} em={}", errorCode.getCode(), message);
        return ok(BaseResponseDTO.error(errorCode.getCode(), message));
    }

    // ── 2. Validation / Bad request → HTTP 400 ───────────────────────────────

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleValidationException(WebExchangeBindException ex) {
        // Lấy field error đầu tiên, fallback global error đầu tiên, fallback generic
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .or(() -> ex.getBindingResult().getGlobalErrors().stream()
                        .map(ge -> ge.getDefaultMessage())
                        .findFirst())
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        log.warn("[EX] Validation: {}", detail);
        return status(HttpStatus.BAD_REQUEST, BaseResponseDTO.error(ErrorCode.BAD_REQUEST.getCode(), detail));
    }

    @ExceptionHandler({ServerWebInputException.class, HttpMessageNotReadableException.class})
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleBadInput(Exception ex) {
        log.warn("[EX] Bad input: {}", ex.getMessage());
        return status(HttpStatus.BAD_REQUEST,
                BaseResponseDTO.error(ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getMessage()));
    }

    // ── 3. Illegal argument → HTTP 400 ───────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[EX] IllegalArgument: {}", ex.getMessage());
        return status(HttpStatus.BAD_REQUEST,
                BaseResponseDTO.error(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage()));
    }

    // ── 4. Catch-all → HTTP 500 ───────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleUnknown(Exception ex) {
        log.error("[EX] Unhandled exception", ex);
        return status(HttpStatus.INTERNAL_SERVER_ERROR, BaseResponseDTO.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static <T> Mono<ResponseEntity<BaseResponseDTO<T>>> ok(BaseResponseDTO<T> body) {
        return Mono.just(ResponseEntity.ok(body));
    }

    private static <T> Mono<ResponseEntity<BaseResponseDTO<T>>> status(HttpStatus httpStatus, BaseResponseDTO<T> body) {
        return Mono.just(ResponseEntity.status(httpStatus).body(body));
    }
}
