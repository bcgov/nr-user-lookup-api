package ca.bc.gov.nrs.userlookup.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("TrustedClientAuthoritiesConverter")
class TrustedClientAuthoritiesConverterTest {

  private static final List<String> ALL_SCOPES = List.of(
      "SCOPE_user-lookup:idir:search", "SCOPE_user-lookup:idir:read",
      "SCOPE_user-lookup:business-bceid:read");

  private static Jwt jwt(String azp, String scope) {
    Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "none").claim("sub", "s");
    if (azp != null) {
      builder.claim("azp", azp);
    }
    if (scope != null) {
      builder.claim("scope", scope);
    }
    return builder.build();
  }

  private static List<String> names(Collection<GrantedAuthority> authorities) {
    return authorities.stream().map(GrantedAuthority::getAuthority).toList();
  }

  @Test
  @DisplayName("a trusted client gets every API scope on top of what its token carries")
  void trustedClientGetsAllScopes() {
    var converter = new TrustedClientAuthoritiesConverter(List.of(" svc-fam ", "svc-other"));

    List<String> granted = names(converter.convert(jwt("svc-fam", "email profile")));

    assertThat(granted).contains("SCOPE_email", "SCOPE_profile").containsAll(ALL_SCOPES);
  }

  @Test
  @DisplayName("an untrusted client keeps only the scopes in its token")
  void untrustedClientKeepsScopeClaimOnly() {
    var converter = new TrustedClientAuthoritiesConverter(List.of("svc-fam"));

    List<String> granted = names(converter.convert(jwt("svc-stranger", "email user-lookup:idir:read")));

    assertThat(granted).containsExactly("SCOPE_email", "SCOPE_user-lookup:idir:read");
  }

  @Test
  @DisplayName("with no trusted clients, scopes alone decide, as before")
  void emptyListChangesNothing() {
    var converter = new TrustedClientAuthoritiesConverter(List.of("", "  "));

    assertThat(converter.hasTrustedClients()).isFalse();
    assertThat(names(converter.convert(jwt("svc-fam", "email")))).containsExactly("SCOPE_email");
    assertThat(names(new TrustedClientAuthoritiesConverter(null).convert(jwt("svc-fam", null)))).isEmpty();
  }

  @Test
  @DisplayName("client_id is used when the token has no azp")
  void fallsBackToClientIdClaim() {
    var converter = new TrustedClientAuthoritiesConverter(List.of("svc-fam"));
    Jwt token = Jwt.withTokenValue("t").header("alg", "none").claim("client_id", "svc-fam").build();

    assertThat(names(converter.convert(token))).containsAll(ALL_SCOPES);
  }
}
