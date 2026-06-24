package ca.bc.gov.nrs.userlookup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the BCeID / IDIR lookup API.
 *
 * <p>This service is a stateless proxy in front of the BC Gov BCeID SOAP web
 * service. Incoming requests are authenticated with Keycloak-issued JWT bearer
 * tokens and authorized per-operation via OAuth2 scopes; outgoing SOAP calls
 * use HTTP Basic auth against the BCeID web service.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class UserLookupApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserLookupApiApplication.class, args);
  }
}
