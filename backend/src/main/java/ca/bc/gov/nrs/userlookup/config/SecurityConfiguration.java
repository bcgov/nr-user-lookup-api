package ca.bc.gov.nrs.userlookup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import ca.bc.gov.nrs.userlookup.security.TrustedClientAuthoritiesConverter;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Web security for the lookup API.
 *
 * <p>This is a pure bearer-token API (no browser session, no cookies), so CSRF
 * is disabled and sessions are stateless. Every request that isn't actuator
 * health/metrics or the OpenAPI docs must carry a valid Keycloak-issued JWT;
 * the {@code JwtDecoder} is auto-configured from
 * {@code spring.security.oauth2.resourceserver.jwt.*} and performs RS256
 * signature, issuer and expiry validation.</p>
 *
 * <p>OAuth2 scopes from the token's {@code scope} claim are exposed as
 * {@code SCOPE_*} authorities by Spring's default converter; endpoints are
 * gated per-operation with {@code @PreAuthorize} (see
 * {@link ca.bc.gov.nrs.userlookup.security.ApiScopes}). Method security is
 * enabled here via {@link EnableMethodSecurity}.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

  /** Paths that are reachable without authentication. */
  private static final String[] PUBLIC_PATHS = {
      "/actuator/health",
      "/actuator/health/**",
      "/actuator/info",
      "/actuator/prometheus",
      "/v3/api-docs",
      "/v3/api-docs/**",
      "/swagger-ui.html",
      "/swagger-ui/**"
  };

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(
            jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    return http.build();
  }

  /**
   * Authorities from a token: its scopes, plus every API scope for the clients named in
   * {@code user-lookup.security.trusted-client-ids} (empty by default). See
   * {@link TrustedClientAuthoritiesConverter} for when that list is the right tool.
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter(
      @Value("${user-lookup.security.trusted-client-ids:}") List<String> trustedClientIds) {
    TrustedClientAuthoritiesConverter authorities =
        new TrustedClientAuthoritiesConverter(trustedClientIds);
    if (authorities.hasTrustedClients()) {
      LoggerFactory.getLogger(SecurityConfiguration.class).info(
          "Trusted-client access is on: {} client id(s) receive every API scope regardless of "
              + "their token's scopes.", trustedClientIds.stream().filter(id -> !id.isBlank()).count());
    }
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
  }
}
