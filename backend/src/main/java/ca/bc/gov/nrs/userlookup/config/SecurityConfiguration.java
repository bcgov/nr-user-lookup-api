package ca.bc.gov.nrs.userlookup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_PATHS).permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
  }
}
