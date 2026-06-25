package ca.bc.gov.nrs.userlookup.security;

/**
 * Per-operation OAuth2 scope expressions for {@code @PreAuthorize}.
 *
 * <p>Spring's default JWT converter maps each space-delimited entry of the
 * token's {@code scope} claim to a {@code SCOPE_<name>} authority, so the
 * expressions below match scopes that must be configured on the Keycloak
 * integration (client scopes assigned to the calling client).</p>
 *
 * <p>Scopes follow a {@code <service>:<resource>:<action>} convention, namespaced to this
 * service to avoid collisions in a shared realm:</p>
 * <ul>
 *   <li>{@code user-lookup:idir:search} — search IDIR users.</li>
 *   <li>{@code user-lookup:idir:read} — read a single IDIR account detail.</li>
 *   <li>{@code user-lookup:business-bceid:read} — read a single Business BCeID account detail.</li>
 * </ul>
 */
public final class ApiScopes {

  private ApiScopes() {
  }

  public static final String IDIR_SEARCH = "hasAuthority('SCOPE_user-lookup:idir:search')";
  public static final String IDIR_READ = "hasAuthority('SCOPE_user-lookup:idir:read')";
  public static final String BUSINESS_BCEID_READ =
      "hasAuthority('SCOPE_user-lookup:business-bceid:read')";
}
