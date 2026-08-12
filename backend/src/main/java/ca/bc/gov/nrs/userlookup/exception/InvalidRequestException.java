package ca.bc.gov.nrs.userlookup.exception;

/**
 * The caller's request is not answerable as asked - a combination of parameters
 * that cannot be resolved, rather than a value the upstream directory rejected.
 *
 * <p>Distinct from {@link UpstreamBusinessException}, which also becomes a 400
 * but reports the directory's verdict on a well-formed request. Keeping them
 * apart means a log line says whether the caller or the directory refused.
 *
 * <p>A dedicated type rather than {@code IllegalArgumentException}: mapping that
 * to 400 globally would also turn an internal one - {@code NumberFormatException}
 * is a subclass - into an apparent client error, hiding a real fault.
 */
public class InvalidRequestException extends RuntimeException {

  public InvalidRequestException(String message) {
    super(message);
  }
}
