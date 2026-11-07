package com.rentaltrack.global.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AuthenticationRequiredException.class)
  public ResponseEntity<ApiErrorResponse> handleUnauthorized(
    AuthenticationRequiredException exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.UNAUTHORIZED,
      "UNAUTHORIZED",
      exception.getMessage(),
      request
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleForbidden(
    AccessDeniedException exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.FORBIDDEN,
      "FORBIDDEN",
      exception.getMessage(),
      request
    );
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(
    ResourceNotFoundException exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.NOT_FOUND,
      "NOT_FOUND",
      exception.getMessage(),
      request
    );
  }

  @ExceptionHandler(
    org.springframework.dao.DataIntegrityViolationException.class
  )
  public ResponseEntity<ApiErrorResponse> handleIntegrity(
    Exception exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.CONFLICT,
      "DATA_CONFLICT",
      "Duplicate or inconsistent data",
      request
    );
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(
    IllegalStateException exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.CONFLICT,
      "INVALID_STATE",
      exception.getMessage(),
      request
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequest(
    IllegalArgumentException exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.BAD_REQUEST,
      "BAD_REQUEST",
      exception.getMessage(),
      request
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
    MethodArgumentNotValidException exception,
    HttpServletRequest request
  ) {
    String message = exception
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .map(error -> error.getField() + ": " + error.getDefaultMessage())
      .distinct()
      .sorted()
      .collect(Collectors.joining(", "));

    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
    ConstraintViolationException exception,
    HttpServletRequest request
  ) {
    String message = exception
      .getConstraintViolations()
      .stream()
      .map(
        violation -> violation.getPropertyPath() + ": " + violation.getMessage()
      )
      .distinct()
      .sorted()
      .collect(Collectors.joining(", "));

    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
  })
  public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
    Exception exception,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.BAD_REQUEST,
      "MALFORMED_REQUEST",
      "Request body or parameter format is invalid",
      request
    );
  }

  private ResponseEntity<ApiErrorResponse> build(
    HttpStatus status,
    String code,
    String message,
    HttpServletRequest request
  ) {
    return ResponseEntity.status(status).body(
      new ApiErrorResponse(
        code,
        message,
        request.getRequestURI(),
        LocalDateTime.now()
      )
    );
  }
}
