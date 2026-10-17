package ca.bc.gov.nrs.userlookup.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Authorities for a caller's token: the scopes it carries, plus every scope of this API when the
 * token belongs to a trusted client.
 *
 * <p>The API's permissions are Keycloak client scopes that the deploy creates in the realm. In a
 * realm a team does not administer - the BC Gov SSO {@code standard} realm, where teams own
 * integrations but cannot add client scopes - no caller can ever carry them. The trusted-client
 * list is the alternative for such realms: the operator names the client ids (the token's
 * {@code azp}, or {@code client_id} when {@code azp} is absent) that may use the whole API.
 * Every other token keeps exactly what its {@code scope} claim grants, so a realm with proper
 * scopes behaves as before, and an empty list changes nothing.
 */
public final class TrustedClientAuthoritiesConverter
    implements Converter<Jwt, Collection<GrantedAuthority>> {

  /** The scopes this API gates its operations on; see {@link ApiScopes}. */
  static final List<String> API_SCOPES = List.of(
      "user-lookup:idir:search", "user-lookup:idir:read", "user-lookup:business-bceid:read");

  private final JwtGrantedAuthoritiesConverter scopeAuthorities = new JwtGrantedAuthoritiesConverter();
  private final Set<String> trustedClientIds;

  /**
   * @param trustedClientIds client ids granted every API scope; blank entries are ignored
   */
  public TrustedClientAuthoritiesConverter(Collection<String> trustedClientIds) {
    this.trustedClientIds = trustedClientIds == null
        ? Set.of()
        : trustedClientIds.stream()
            .filter(id -> id != null && !id.isBlank())
            .map(String::trim)
            .collect(Collectors.toUnmodifiableSet());
  }

  /** Whether any client is trusted; false means scopes alone decide. */
  public boolean hasTrustedClients() {
    return !trustedClientIds.isEmpty();
  }

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    Collection<GrantedAuthority> fromScopes = scopeAuthorities.convert(jwt);
    if (fromScopes != null) {
      authorities.addAll(fromScopes);
    }
    String client = clientOf(jwt);
    if (client != null && trustedClientIds.contains(client)) {
      for (String scope : API_SCOPES) {
        authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
      }
    }
    return authorities;
  }

  private static String clientOf(Jwt jwt) {
    String azp = jwt.getClaimAsString("azp");
    if (azp != null && !azp.isBlank()) {
      return azp.trim();
    }
    String clientId = jwt.getClaimAsString("client_id");
    return clientId == null || clientId.isBlank() ? null : clientId.trim();
  }
}
