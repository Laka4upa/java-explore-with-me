package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.model.dto.ApiError;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    private static final String INCORRECT_REQUEST_REASON = "Incorrectly made request.";
    private static final String INTEGRITY_VIOLATION_REASON = "Integrity constraint has been violated.";
    private static final String NOT_FOUND_REASON = "The required object was not found.";
    private static final String CONDITIONS_NOT_MET_REASON = "For the requested operation the conditions are not met.";

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleEntityNotFound(EntityNotFoundException e) {
        log.error("Entity not found: {}", e.getMessage());
        return createApiError(HttpStatus.NOT_FOUND, NOT_FOUND_REASON, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.error("Conflict: {}", e.getMessage());
        return createApiError(HttpStatus.CONFLICT, CONDITIONS_NOT_MET_REASON, e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(ValidationException e) {
        log.error("Validation error: {}", e.getMessage());
        return createApiError(HttpStatus.BAD_REQUEST, INCORRECT_REQUEST_REASON, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                .collect(Collectors.joining("; "));

        log.error("Validation error: {}", message);
        return createApiError(HttpStatus.BAD_REQUEST, INCORRECT_REQUEST_REASON, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("Failed to convert value of type %s to required type %s",
                e.getValue(), e.getRequiredType().getSimpleName());

        log.error("Type mismatch: {}", message);
        return createApiError(HttpStatus.BAD_REQUEST, INCORRECT_REQUEST_REASON, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage());
        return createApiError(HttpStatus.CONFLICT, INTEGRITY_VIOLATION_REASON,
                "could not execute statement; SQL [n/a]; constraint violation");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParams(MissingServletRequestParameterException e) {
        String message = String.format("Missing required parameter: %s", e.getParameterName());
        log.error("Missing parameter: {}", message);
        return createApiError(HttpStatus.BAD_REQUEST, INCORRECT_REQUEST_REASON, message);
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestExceptions(Exception e) {
        String message = e instanceof DateTimeParseException
                ? "Invalid date format. Use 'yyyy-MM-dd HH:mm:ss'"
                : e.getMessage();

        log.error("Bad request error: {}", message);
        return createApiError(HttpStatus.BAD_REQUEST, INCORRECT_REQUEST_REASON, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleInternalError(Exception e) {
        log.error("Internal server error: {}", e.getMessage(), e);
        return ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .reason("Internal server error")
                .message(e.getMessage())
                .errors(Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toList()))
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    private ApiError createApiError(HttpStatus status, String reason, String message) {
        return ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}