package com.comma.soomteum.global.exception;

import com.comma.soomteum.global.response.ApiResponse;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustom(CustomException ex, ServerHttpRequest request) {
        log.error("[CustomException] code={}, msg={}, detail={}",
                ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), ex.getMessage());
        var ec = ex.getErrorCode();
        var body = ApiResponse.fail(ex, request.getPath().value()); // ★ path 전달
        return ResponseEntity.status(ec.getHttpStatus()).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleEntityNotFound(EntityNotFoundException ex, ServerHttpRequest request) {
        log.error("[EntityNotFound] {}", ex.getMessage(), ex);
        var custom = new CustomException(ErrorCode.NOT_FOUND_END_POINT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(custom, request.getPath().value()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException ex, ServerHttpRequest request) {
        log.error("[IllegalState] {}", ex.getMessage(), ex);
        var custom = new CustomException(ErrorCode.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(custom, request.getPath().value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception ex, ServerHttpRequest request) {
        log.error("[Unhandled] {}", ex.getMessage(), ex);
        var custom = new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(custom, request.getPath().value()));
    }
}