package com.transferapp.userservice.exception;

import com.transferapp.userservice.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ApiError> handleUserProfileNotFoundException(
            UserProfileNotFoundException exception,
            HttpServletRequest request){
        HttpStatus status=HttpStatus.NOT_FOUND;
        ApiError error=new ApiError(
                Instant.now(),
                status.value(),
                "USER_PROFILE_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
        return  ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(UserProfileAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserProfileAlreadyExists(
            UserProfileAlreadyExistsException exception,
            HttpServletRequest request
    ){
        HttpStatus status=HttpStatus.CONFLICT;
        ApiError error=new ApiError(
                Instant.now(),
                status.value(),
                "USER_PROFILE_ALREADY_EXISTS",
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ){
        HttpStatus status=HttpStatus.BAD_REQUEST;
        ApiError error=new ApiError(
                Instant.now(),
                status.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI());
        return ResponseEntity
                .status(status)
                .body(error);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request){
        HttpStatus status=HttpStatus.BAD_REQUEST;

        ApiError error=new ApiError(
                Instant.now(),
                status.value(),
                "INVALID_REQUEST_BODY",
                "Request body is missing or malformed",
                request.getRequestURI());
        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

}
