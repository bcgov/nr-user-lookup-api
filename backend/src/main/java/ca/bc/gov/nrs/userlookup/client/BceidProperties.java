package ca.bc.gov.nrs.userlookup.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the BCeID SOAP web service. Bound from the
 * {@code bceid.web-service.*} properties (see application.yml).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bceid.web-service")
public class BceidProperties {

  /** Service URL. May be the {@code ...?WSDL} form; the query is stripped for direct calls. */
  private String url;

  /** Online service id supplied in every request payload. */
  private String onlineServiceId;

  /**
   * Internal requester user GUID (an Internal/IDIR service identity) supplied as
   * the {@code requesterUserGuid} on every outbound lookup. This is a fixed
   * server-side configuration value — callers never provide it.
   */
  private String requesterUserGuid;

  /** HTTP Basic auth username for the SOAP call. */
  private String username;

  /** HTTP Basic auth password for the SOAP call. */
  private String password;

  /** SOAPAction header prefix; the operation name is appended per call. */
  private String soapActionPrefix;

  /** The bare endpoint URL with any {@code ?WSDL} (or other) query removed. */
  public String getEndpointUrl() {
    if (url == null) {
      return null;
    }
    int q = url.indexOf('?');
    return q >= 0 ? url.substring(0, q) : url;
  }
}
