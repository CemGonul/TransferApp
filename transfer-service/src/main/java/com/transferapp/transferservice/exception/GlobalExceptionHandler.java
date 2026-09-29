package com.transferapp.transferservice.exception;

import com.transferapp.transferservice.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleAccountAlreadyExists(
            AccountAlreadyExistsException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.CONFLICT;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "ACCOUNT_ALREADY_EXISTS",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.CONFLICT;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "DATA_INTEGRITY_VIOLATION",
                "Data conflicts with an existing record",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        String message = exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(
            AccountNotFoundException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "NOT_FOUND",
                "Account not found",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccountAccessDenied(
            AccountAccessDeniedException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.FORBIDDEN;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "FORBIDDEN",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "BAD_REQUEST",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(SelfTransferException.class)
    public ResponseEntity<ApiError> handleSelfTransfer(
            SelfTransferException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "SELF_TRANSFER_NOT_ALLOWED",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ApiError> handleCurrencyMismatch(
            CurrencyMismatchException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "CURRENCY_MISMATCH",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(
            InsufficientBalanceException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "INSUFFICIENT_BALANCE",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<ApiError> handleTransferNotFound(
            TransferNotFoundException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "TRANSFER_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(TransferAccessDeniedException.class)
    public ResponseEntity<ApiError> handleTransferAccessDenied(
            TransferAccessDeniedException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.FORBIDDEN;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "FORBIDDEN",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ResponseEntity<ApiError> handleIdempotencyKeyReuse(
            IdempotencyKeyReuseException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.CONFLICT;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "IDEMPOTENCY_KEY_REUSE",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(IdempotencyRequestInProgressException.class)
    public ResponseEntity<ApiError> handleIdempotencyRequestInProgress(
            IdempotencyRequestInProgressException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.CONFLICT;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "IDEMPOTENCY_REQUEST_IN_PROGRESS",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request
    ) {

        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "RATE_LIMIT_EXCEEDED",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }
}
