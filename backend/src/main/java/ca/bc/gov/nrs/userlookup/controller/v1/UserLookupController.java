package ca.bc.gov.nrs.userlookup.controller.v1;

import ca.bc.gov.nrs.userlookup.dto.BceidUserResponse;
import ca.bc.gov.nrs.userlookup.dto.IdirUserResponse;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersQuery;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersResponse;
import ca.bc.gov.nrs.userlookup.dto.SearchUserParameterType;
import ca.bc.gov.nrs.userlookup.security.ApiScopes;
import ca.bc.gov.nrs.userlookup.service.UserLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BCeID / IDIR lookup endpoints, version 1. Every endpoint requires a
 * Keycloak bearer token carrying the operation's scope (see {@link ApiScopes}).
 *
 * <p>The API is versioned in the path under {@code /api/v1}; introduce a
 * {@code controller.v2} package alongside this one for any future breaking
 * change so existing clients keep working.</p>
 */
@RestController
@RequestMapping(UserLookupController.BASE_PATH)
@RequiredArgsConstructor
@Tag(name = "User Lookup", description = "IDIR / BCeID user lookups via the BCeID SOAP service")
@SecurityRequirement(name = "bearerAuth")
public class UserLookupController {

  /** Versioned base path for these endpoints. */
  static final String BASE_PATH = "/api/v1/user-lookup";

  private final UserLookupService userLookupService;

  @GetMapping("/idir-account-detail")
  @PreAuthorize(ApiScopes.IDIR_READ)
  @Operation(summary = "Get IDIR user account detail by userId (exact match)")
  public IdirUserResponse verifyIdirUserByAccountDetail(
      @RequestParam("userId") String userId) {
    return userLookupService.verifyIdirUserByAccountDetail(userId);
  }

  @PostMapping("/idir-users/search")
  @PreAuthorize(ApiScopes.IDIR_SEARCH)
  @Operation(summary = "Search IDIR users",
      description = "Searches IDIR users by firstName, lastName, or userId via the BCeID web "
          + "service (partial match allowed). At least one search field is required.")
  public SearchIdirUsersResponse searchIdirUsers(@Valid SearchIdirUsersQuery query) {
    return userLookupService.searchIdirUsers(query);
  }

  @GetMapping("/businessBceid")
  @PreAuthorize(ApiScopes.BUSINESS_BCEID_READ)
  @Operation(summary = "Get BCeID business user account detail by userId or userGuid (exact match)")
  public BceidUserResponse verifyBusinessBceidUser(
      @RequestParam("searchUserBy") SearchUserParameterType searchUserBy,
      @RequestParam("searchValue") String searchValue) {
    return userLookupService.verifyBusinessBceidUser(searchUserBy, searchValue);
  }
}
