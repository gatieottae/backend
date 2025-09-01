package com.gatieottae.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "표준 에러 응답")
public class ApiErrorResponse {

    @Schema(description = "에러 코드 키", example = "VALIDATION_FAILED")
    String code;

    @Schema(description = "HTTP 상태 코드", example = "400")
    int status;

    @Schema(description = "사람이 읽을 수 있는 기본 메시지", example = "요청 값이 올바르지 않습니다.")
    String message;

    @Schema(description = "에러가 발생한 시각(UTC 기준일 수도 있음)")
    OffsetDateTime timestamp;

    @Schema(description = "필드 검증 오류 목록")
    List<FieldError> errors;

    @Value
    @Builder
    public static class FieldError {
        @Schema(description = "필드명", example = "username")
        String field;

        @Schema(description = "거절 사유", example = "size must be between 3 and 50")
        String reason;

        @Schema(description = "거절된 값", example = "ab")
        String rejectedValue;
    }

    public static ApiErrorResponse of(ErrorCode code, String message, List<FieldError> errors) {
        return ApiErrorResponse.builder()
                .code(code.name())
                .status(code.status.value())
                .message(message != null ? message : code.defaultMessage)
                .timestamp(OffsetDateTime.now())
                .errors(errors)
                .build();
    }

    public static ApiErrorResponse of(ErrorCode code) {
        return of(code, null, null);
    }
}