package ca.bc.gov.nrs.userlookup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.bc.gov.nrs.userlookup.dto.IdirUserResponse;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersResponse;
import ca.bc.gov.nrs.userlookup.service.UserLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The trusted-client list in action: a token with no API scopes but a trusted {@code azp} reaches
 * every operation; any other client is still gated by scopes. Authorities are computed through the
 * application's own {@link JwtAuthenticationConverter}, so this covers the property binding and the
 * security wiring, not only the converter class.
 */
@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://localhost/auth/realms/test",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://localhost/auth/realms/test/protocol/openid-connect/certs",
    "user-lookup.security.trusted-client-ids=service-account-team-fam, svc-other"
})
@AutoConfigureMockMvc
class TrustedClientSecurityTest {

  private static final String SEARCH_URL = "/api/v1/user-lookup/idir-users/search";
  private static final String DETAIL_URL = "/api/v1/user-lookup/idir-account-detail";
  private static final String BCEID_URL = "/api/v1/user-lookup/businessBceid";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtAuthenticationConverter jwtAuthenticationConverter;
  @MockitoBean private UserLookupService userLookupService;

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor tokenFrom(String azp) {
    return jwt()
        .jwt(j -> j.claim("azp", azp).claim("scope", "email profile"))
        .authorities(j -> jwtAuthenticationConverter.convert(j).getAuthorities());
  }

  @Test
  void trustedClientReachesSearchDetailAndBceidWithoutApiScopes() throws Exception {
    when(userLookupService.searchIdirUsers(any())).thenReturn(new SearchIdirUsersResponse());
    when(userLookupService.verifyIdirUser(any(), any())).thenReturn(new IdirUserResponse());

    mockMvc.perform(post(SEARCH_URL).with(tokenFrom("service-account-team-fam")).param("userId", "jdoe"))
        .andExpect(status().isOk());
    mockMvc.perform(get(DETAIL_URL).with(tokenFrom("svc-other")).param("userId", "jdoe"))
        .andExpect(status().isOk());
  }

  @Test
  void untrustedClientIsStillForbidden() throws Exception {
    mockMvc.perform(post(SEARCH_URL).with(tokenFrom("service-account-team-stranger")).param("userId", "jdoe"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(BCEID_URL).with(tokenFrom("service-account-team-stranger"))
            .param("searchUserBy", "userId").param("searchValue", "x"))
        .andExpect(status().isForbidden());
  }

  @Test
  void noTokenIsStillUnauthorized() throws Exception {
    mockMvc.perform(post(SEARCH_URL).param("userId", "jdoe")).andExpect(status().isUnauthorized());
  }
}
