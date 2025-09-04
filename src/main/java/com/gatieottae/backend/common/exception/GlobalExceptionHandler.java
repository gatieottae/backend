package com.gatieottae.backend.common.exception;

import com.gatieottae.backend.domain.group.exception.GroupException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * 글로벌 예외 처리기 (표준 에러 응답 규격: ApiErrorResponse + ErrorCode)
 *
 * - 400 VALIDATION_FAILED : DTO 바인딩/검증 실패
 * - 400 BAD_REQUEST       : 도메인 규칙 위반 등 일반적인 요청 오류
 * - 404 NOT_FOUND         : 리소스를 찾을 수 없음
 * - 409 CONFLICT          : 중복/상태 충돌, DB 제약 위반
 * - 500 INTERNAL_ERROR    : 예기치 못한 서버 오류
 *
 * 운영 편의를 위해 예외별 로그 레벨을 구분:
 * - 사용자가 유발 가능한 오류(400/404/409)는 warn
 * - 서버/예상치 못한 오류(500)는 error (스택트레이스 포함)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ======================= 400 Validation ======================= */

    // @RequestBody DTO 바인딩/검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return validationResponse(ex.getBindingResult(), ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultMessage);
    }

    // @ModelAttribute / @RequestParam / PathVariable 바인딩/검증 실패
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex) {
        log.warn("Binding failed: {}", ex.getMessage());
        return validationResponse(ex.getBindingResult(), ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.defaultMessage);
    }

    // 메서드 파라미터 수준의 제약(@RequestParam, PathVariable 등) 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(
                ErrorCode.VALIDATION_FAILED,
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status).body(body);
    }

    /* ==================== 404 Not Found (커스텀) ==================== */

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        ErrorCode code = ex.code != null ? ex.code : ErrorCode.NOT_FOUND;
        log.warn("Not found: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(code, ex.getMessage(), null);
        return ResponseEntity.status(code.status).body(body);
    }

    /* ================= 409 Conflict / 400 BadRequest ================ */

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        ErrorCode code = ex.code != null ? ex.code : ErrorCode.CONFLICT;
        log.warn("Conflict: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(code, ex.getMessage(), null);
        return ResponseEntity.status(code.status).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        ErrorCode code = ex.code != null ? ex.code : ErrorCode.BAD_REQUEST;
        log.warn("Bad request: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(code, ex.getMessage(), null);
        return ResponseEntity.status(code.status).body(body);
    }

    // 임시: IllegalStateException을 충돌로 매핑 (향후 도메인 예외로 대체 권장)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state (mapped to CONFLICT): {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(ErrorCode.CONFLICT, ex.getMessage(), null);
        return ResponseEntity.status(ErrorCode.CONFLICT.status).body(body);
    }

    // DB 무결성 위반(유니크 키 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Data integrity violation: {}", message);
        ApiErrorResponse body = ApiErrorResponse.of(ErrorCode.CONFLICT, message, null);
        return ResponseEntity.status(ErrorCode.CONFLICT.status).body(body);
    }

    /* ======================= 500 Unexpected ======================== */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        // 운영 문제 추적을 위해 스택 포함 error 로그
        log.error("Unexpected error", ex);
        ApiErrorResponse body = ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, ex.getMessage(), null);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status).body(body);
    }

    @ExceptionHandler(GroupException.class)
    public ResponseEntity<ApiErrorResponse> handleGroupException(GroupException ex) {
        log.warn("Group exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        int httpStatus = switch (ex.getErrorCode()) {
            case INVALID_CODE -> HttpStatus.NOT_FOUND.value();
            case ALREADY_MEMBER -> HttpStatus.CONFLICT.value();
            case GROUP_NAME_DUPLICATED -> HttpStatus.CONFLICT.value();
            case GROUP_NOT_FOUND -> HttpStatus.NOT_FOUND.value();
            case NO_PERMISSION -> HttpStatus.FORBIDDEN.value();
        };

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(ex.getErrorCode().name())
                .status(httpStatus)
                .message(ex.getMessage())
                .errors(null)
                .build();

        return ResponseEntity.status(httpStatus).body(body);
    }

    /* ========================= Helpers ============================ */

    private ResponseEntity<ApiErrorResponse> validationResponse(BindingResult bindingResult,
                                                                ErrorCode code,
                                                                String message) {
        List<ApiErrorResponse.FieldError> fields = bindingResult.getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .collect(toList());
        ApiErrorResponse body = ApiErrorResponse.of(code, message, fields);
        return ResponseEntity.status(code.status).body(body);
    }

    private ApiErrorResponse.FieldError toFieldError(FieldError e) {
        Object rejected = e.getRejectedValue();
        return ApiErrorResponse.FieldError.builder()
                .field(e.getField())
                .reason(e.getDefaultMessage())
                .rejectedValue(rejected == null ? null : String.valueOf(rejected))
                .build();
    }
}