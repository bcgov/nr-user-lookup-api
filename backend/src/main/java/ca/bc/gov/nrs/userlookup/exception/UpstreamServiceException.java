package ca.bc.gov.nrs.userlookup.exception;

/**
 * Raised for BCeID transport/configuration failures (network error, missing
 * credentials, empty response). Surfaces as HTTP 500, matching the proxy.
 */
public class UpstreamServiceException extends RuntimeException {

  public UpstreamServiceException(String message) {
    super(message);
  }
}
