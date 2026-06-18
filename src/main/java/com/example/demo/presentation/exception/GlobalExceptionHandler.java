package com.example.demo.presentation.exception;

import com.example.demo.application.dto.BaseResponseDTO;
import com.example.demo.application.exception.AppException;
import com.example.demo.application.exception.ErrorCode;
import com.example.demo.application.exception.ErrorMessageRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ── 1. Business exception ─────────────────────────────────────────────────

    @ExceptionHandler(AppException.class)
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = errorMessageRegistry.getMessage(errorCode); // từ Redis, fallback enum
        log.warn("[EX] AppException: ec={} em={}", errorCode.getCode(), message);
        return ok(BaseResponseDTO.error(errorCode.getCode(), message));
    }

    // ── 2. Validation / Bad request ───────────────────────────────────────────

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
        return ok(BaseResponseDTO.error(ErrorCode.BAD_REQUEST.getCode(), detail));
    }

    @ExceptionHandler({ServerWebInputException.class, HttpMessageNotReadableException.class})
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleBadInput(Exception ex) {
        log.warn("[EX] Bad input: {}", ex.getMessage());
        return ok(BaseResponseDTO.error(ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getMessage()));
    }

    // ── 3. Illegal argument ───────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[EX] IllegalArgument: {}", ex.getMessage());
        return ok(BaseResponseDTO.error(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage()));
    }

    // ── 4. Catch-all ──────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<BaseResponseDTO<Void>>> handleUnknown(Exception ex) {
        log.error("[EX] Unhandled exception", ex);
        return ok(BaseResponseDTO.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    // ── Helper: HTTP status luôn 200 ─────────────────────────────────────────

    private static <T> Mono<ResponseEntity<BaseResponseDTO<T>>> ok(BaseResponseDTO<T> body) {
        return Mono.just(ResponseEntity.ok(body));
    }
}
