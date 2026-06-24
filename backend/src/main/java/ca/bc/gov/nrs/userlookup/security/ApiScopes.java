package ca.bc.gov.nrs.userlookup.security;

/**
 * Per-operation OAuth2 scope expressions for {@code @PreAuthorize}.
 *
 * <p>Spring's default JWT converter maps each space-delimited entry of the
 * token's {@code scope} claim to a {@code SCOPE_<name>} authority, so the
 * expressions below match scopes that must be configured on the Keycloak
 * integration (client scopes assigned to the calling client).</p>
 *
 * <ul>
 *   <li>{@code IDIR_SEARCH} — search IDIR users.</li>
 *   <li>{@code IDIR_READ} — read a single IDIR account detail.</li>
 *   <li>{@code BCEID_READ} — read a single BCeID business account detail.</li>
 * </ul>
 */
public final class ApiScopes {

  private ApiScopes() {
  }

  public static final String IDIR_SEARCH = "hasAuthority('SCOPE_IDIR_SEARCH')";
  public static final String IDIR_READ = "hasAuthority('SCOPE_IDIR_READ')";
  public static final String BUSINESS_BCEID_READ = "hasAuthority('SCOPE_BUSINESS_BCEID_READ')";
}
