package ca.bc.gov.nrs.userlookup.controller;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.bc.gov.nrs.userlookup.dto.IdirUserResponse;
import ca.bc.gov.nrs.userlookup.dto.SearchUserParameterType;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersResponse;
import ca.bc.gov.nrs.userlookup.service.UserLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies per-operation scope enforcement and request validation on the
 * endpoints. The {@link UserLookupService} (and the SOAP call beneath it)
 * is mocked so the tests exercise only the web/security layer.
 */
@SpringBootTest(properties = {
    // issuer-uri has no default in application.yml; supply dummy OAuth2 config so the
    // context loads. The JwtDecoder is lazy (no network) and tests use the jwt()
    // post-processor, so these values are never dereferenced.
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://localhost/auth/realms/test",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://localhost/auth/realms/test/protocol/openid-connect/certs"
})
@AutoConfigureMockMvc
class UserLookupControllerSecurityTest {

  private static final String SEARCH_URL = "/api/v1/user-lookup/idir-users/search";
  private static final String DETAIL_URL = "/api/v1/user-lookup/idir-account-detail";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserLookupService userLookupService;

  private static SimpleGrantedAuthority scope(String name) {
    return new SimpleGrantedAuthority("SCOPE_" + name);
  }

  @Test
  void searchWithRequiredScopeReturns200() throws Exception {
    when(userLookupService.searchIdirUsers(any())).thenReturn(new SearchIdirUsersResponse());

    mockMvc.perform(post(SEARCH_URL)
            .with(jwt().authorities(scope("user-lookup:idir:search")))
            .param("firstName", "John"))
        .andExpect(status().isOk());
  }

  @Test
  void searchWithoutTokenReturns401() throws Exception {
    mockMvc.perform(post(SEARCH_URL)
            .param("firstName", "John"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void searchWithWrongScopeReturns403() throws Exception {
    mockMvc.perform(post(SEARCH_URL)
            .with(jwt().authorities(scope("user-lookup:idir:read")))
            .param("firstName", "John"))
        .andExpect(status().isForbidden());
  }

  @Test
  void searchWithNoSearchFieldReturns400() throws Exception {
    mockMvc.perform(post(SEARCH_URL)
            .with(jwt().authorities(scope("user-lookup:idir:search"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void accountDetailWithRequiredScopeReturns200() throws Exception {
    when(userLookupService.verifyIdirUser(any(), any())).thenReturn(new IdirUserResponse());

    mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:idir:read")))
            .param("userId", "jdoe"))
        .andExpect(status().isOk());
  }

  @Test
  void accountDetailWithWrongScopeReturns403() throws Exception {
    mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:business-bceid:read")))
            .param("userId", "jdoe"))
        .andExpect(status().isForbidden());
  }

  @Test
  void accountDetailByGuidUsesTheSameScope() {
    // Same account detail from the same source, reached by a different key, so
    // it is not a separate permission.
    when(userLookupService.verifyIdirUser(any(), any())).thenReturn(new IdirUserResponse());

    assertThatCode(() -> mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:idir:read")))
            .param("userGuid", "0C93B7EB34654CB2B5F6EC399990D466"))
        .andExpect(status().isOk()))
        .doesNotThrowAnyException();
  }

  @Test
  void accountDetailByGuidWithWrongScopeReturns403() throws Exception {
    mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:business-bceid:read")))
            .param("userGuid", "0C93B7EB34654CB2B5F6EC399990D466"))
        .andExpect(status().isForbidden());
  }

  @Test
  void accountDetailByGuidLooksUpByGuid() throws Exception {
    // Guards the wiring: passing the GUID through as a userId would silently
    // look up a login name and find nobody.
    when(userLookupService.verifyIdirUser(any(), any())).thenReturn(new IdirUserResponse());

    mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:idir:read")))
            .param("userGuid", "GUID1"))
        .andExpect(status().isOk());

    verify(userLookupService).verifyIdirUser(SearchUserParameterType.userGuid, "GUID1");
  }

  @Test
  void accountDetailWithBothIdentifiersReturns400() throws Exception {
    // Two identifiers that disagree have no single right answer, so neither is
    // silently preferred.
    mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:idir:read")))
            .param("userId", "jdoe")
            .param("userGuid", "GUID1"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userLookupService);
  }

  @Test
  void accountDetailWithNeitherIdentifierReturns400() throws Exception {
    mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:idir:read"))))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userLookupService);
  }

  @Test
  void accountDetailBadRequestSaysWhy() throws Exception {
    // The service does not expose exception messages by default, so the reason
    // has to be carried in the response body deliberately.
    mockMvc.perform(get(DETAIL_URL)
            .with(jwt().authorities(scope("user-lookup:idir:read"))))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(org.hamcrest.Matchers.containsString(
            "exactly one of userId or userGuid")));
  }
}
