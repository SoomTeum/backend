package com.comma.soomteum.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "code", "status", "message", "path", "timestamp", "detail" })
@Schema(name = "ExceptionDto", description = "표준 에러 응답")
public class ExceptionDto {

    @NotNull
    @Schema(description = "애플리케이션 에러 코드", example = "502_001")
    int code;              // ex) "502_001"

    @Schema(description = "HTTP 상태 코드", example = "502")
    Integer status;           // ex) 502

    @NotNull
    @Schema(description = "메시지", example = "외부 관광 API 클라이언트 오류 발생")
    String message;

    @Schema(description = "요청 경로", example = "/api/kor/area-based")
    String path;

    @Schema(description = "발생 시각(UTC)", example = "2025-08-13T05:43:21Z")
    OffsetDateTime timestamp;

    @Schema(description = "상세(업스트림 바디 일부/디버그 메시지)")
    String detail;

    public static ExceptionDto of(ErrorCode ec, String path, String detail) {
        return ExceptionDto.builder()
                .code(ec.getCode())
                .status(ec.getHttpStatus().value())
                .message(ec.getMessage())
                .path(path)
                .timestamp(OffsetDateTime.now())
                .detail(detail)
                .build();
    }
}
