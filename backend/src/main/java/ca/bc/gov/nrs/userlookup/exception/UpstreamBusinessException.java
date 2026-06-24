package ca.bc.gov.nrs.userlookup.exception;

import lombok.Getter;

/**
 * Raised when the BCeID SOAP response reports a business failure
 * ({@code code = Failed} with a non-ignored {@code failureCode}). Surfaces as
 * HTTP 400 carrying the SOAP {@code code}/{@code failureCode}/{@code message},
 * matching the proxy's 400 response body.
 */
@Getter
public class UpstreamBusinessException extends RuntimeException {

  private final String code;
  private final String failureCode;

  public UpstreamBusinessException(String code, String failureCode, String message) {
    super(message);
    this.code = code;
    this.failureCode = failureCode;
  }
}
