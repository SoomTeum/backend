package com.comma.soomteum.global.response;

// package com.comma.soomteum.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micrometer.common.lang.Nullable;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        @JsonIgnore HttpStatus httpStatus,
        boolean success,
        @Nullable T data,
        @Nullable ExceptionDto error
) {
    public static <T> ApiResponse<T> ok(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.OK, true, data, null);
    }

    public static <T> ApiResponse<T> created(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.CREATED, true, data, null);
    }

    // 기존 (path 없는 버전이 필요하면 남겨두고 내부에서 호출해도 됨)
    public static <T> ApiResponse<T> fail(final CustomException e, final String path) {
        return new ApiResponse<>(
                e.getErrorCode().getHttpStatus(),
                false,
                null,
                ExceptionDto.of(e.getErrorCode(), path, e.getMessage())
        );
    }
}
