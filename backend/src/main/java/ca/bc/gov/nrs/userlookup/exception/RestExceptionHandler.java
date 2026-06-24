package ca.bc.gov.nrs.userlookup.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Centralised error handling. Translates BCeID SOAP errors and request
 * validation failures into a consistent {@link ApiError} body.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  @ExceptionHandler(UpstreamBusinessException.class)
  protected ResponseEntity<Object> handleUpstreamBusiness(UpstreamBusinessException ex) {
    ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage());
    apiError.setCode(ex.getCode());
    apiError.setFailureCode(ex.getFailureCode());
    log.info("BCeID business error: code={}, failureCode={}, message={}",
        ex.getCode(), ex.getFailureCode(), ex.getMessage());
    return new ResponseEntity<>(apiError, apiError.getStatus());
  }

  @ExceptionHandler(UpstreamServiceException.class)
  protected ResponseEntity<Object> handleUpstreamService(UpstreamServiceException ex) {
    ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    log.error("BCeID service error: {}", ex.getMessage(), ex);
    return new ResponseEntity<>(apiError, apiError.getStatus());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
    ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Validation error");
    List<String> errors = new ArrayList<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
    }
    apiError.setErrors(errors);
    log.info("Validation error: {}", errors);
    return new ResponseEntity<>(apiError, apiError.getStatus());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Validation error");
    List<String> errors = new ArrayList<>();
    ex.getBindingResult().getFieldErrors().forEach(
        e -> errors.add(e.getField() + ": " + e.getDefaultMessage()));
    for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
      errors.add(globalError.getDefaultMessage());
    }
    apiError.setErrors(errors);
    log.info("Validation error: {}", errors);
    return new ResponseEntity<>(apiError, apiError.getStatus());
  }
}
