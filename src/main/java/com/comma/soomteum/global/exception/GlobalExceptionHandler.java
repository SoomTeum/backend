package com.comma.soomteum.global.exception;

import com.comma.soomteum.global.response.ApiResponse;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest; // ★ 여기!
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustom(CustomException ex, HttpServletRequest request) {
        log.error("[CustomException] code={}, msg={}, detail={}, path={}",
                ex.getErrorCode().getCode(), ex.getErrorCode().getMessage(), ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(ex, request.getRequestURI()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.error("[EntityNotFound] {}", ex.getMessage(), ex);
        var custom = new CustomException(ErrorCode.NOT_FOUND_END_POINT, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(custom, request.getRequestURI()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.error("[IllegalState] {}", ex.getMessage(), ex);
        var custom = new CustomException(ErrorCode.BAD_REQUEST, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(custom, request.getRequestURI()));
    }

    // 업스트림 예외 매핑
    @ExceptionHandler(com.comma.soomteum.domain.place.Service.KorApiCaller.Upstream4xxException.class)
    public ResponseEntity<ApiResponse<?>> handleUpstream4xx(
            com.comma.soomteum.domain.place.Service.KorApiCaller.Upstream4xxException ex,
            HttpServletRequest request
    ) {
        var custom = new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(custom, request.getRequestURI()));
    }

    @ExceptionHandler(com.comma.soomteum.domain.place.Service.KorApiCaller.Upstream5xxException.class)
    public ResponseEntity<ApiResponse<?>> handleUpstream5xx(
            com.comma.soomteum.domain.place.Service.KorApiCaller.Upstream5xxException ex,
            HttpServletRequest request
    ) {
        var custom = new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(custom, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("[Unhandled] {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        var custom = new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(custom, request.getRequestURI()));
    }
}
