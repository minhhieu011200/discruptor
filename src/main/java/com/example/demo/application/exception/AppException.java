package com.example.demo.application.exception;

import lombok.Getter;

/**
 * Business exception tập trung — throw từ bất kỳ layer nào.
 *
 * <pre>
 *   throw new AppException(ErrorCode.SYMBOL_NOT_FOUND);
 *   throw new AppException(ErrorCode.BAD_REQUEST, "ids không được rỗng");
 * </pre>
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
