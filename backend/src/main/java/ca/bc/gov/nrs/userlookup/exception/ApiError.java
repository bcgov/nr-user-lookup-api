package ca.bc.gov.nrs.userlookup.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

/**
 * Standardized error response body. Carries the BCeID SOAP {@code code} /
 * {@code failureCode} for business errors (matching the proxy's 400 body) and a
 * list of validation messages for request-validation failures.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError implements Serializable {

  private HttpStatus status;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
  private LocalDateTime timestamp;

  private String message;

  /** BCeID SOAP result code (business errors only). */
  private String code;

  /** BCeID SOAP failure code (business errors only). */
  private String failureCode;

  /** Validation messages (validation errors only). */
  private List<String> errors;

  public ApiError(HttpStatus status) {
    this.timestamp = LocalDateTime.now();
    this.status = status;
  }

  public ApiError(HttpStatus status, String message) {
    this(status);
    this.message = message;
  }
}
