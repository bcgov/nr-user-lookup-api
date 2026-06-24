package ca.bc.gov.nrs.userlookup.service;

import ca.bc.gov.nrs.userlookup.client.BceidSoapClient;
import ca.bc.gov.nrs.userlookup.client.BceidProperties;
import ca.bc.gov.nrs.userlookup.client.soap.AccountDetailRequest;
import ca.bc.gov.nrs.userlookup.client.soap.AccountMatch;
import ca.bc.gov.nrs.userlookup.client.soap.BceidAccount;
import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailRequest;
import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailResult;
import ca.bc.gov.nrs.userlookup.client.soap.InternalAccountSearchRequest;
import ca.bc.gov.nrs.userlookup.client.soap.MatchProperty;
import ca.bc.gov.nrs.userlookup.client.soap.Pagination;
import ca.bc.gov.nrs.userlookup.client.soap.ResultPagination;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountRequest;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountResult;
import ca.bc.gov.nrs.userlookup.client.soap.Sort;
import ca.bc.gov.nrs.userlookup.client.soap.StringValue;
import ca.bc.gov.nrs.userlookup.dto.BceidUserResponse;
import ca.bc.gov.nrs.userlookup.dto.IdirUserResponse;
import ca.bc.gov.nrs.userlookup.dto.RequesterAccountTypeCode;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUserResItem;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersQuery;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersResponse;
import ca.bc.gov.nrs.userlookup.dto.SearchMatchMode;
import ca.bc.gov.nrs.userlookup.dto.SearchUserParameterType;
import ca.bc.gov.nrs.userlookup.exception.UpstreamBusinessException;
import ca.bc.gov.nrs.userlookup.exception.UpstreamServiceException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Business logic ported from the original Node proxy service.
 *
 * <p>Error semantics preserved from the proxy: transport/config failures →
 * {@link UpstreamServiceException} (HTTP 500); a SOAP {@code code = Failed} (with a
 * non-ignored {@code failureCode}) → {@link UpstreamBusinessException} (HTTP 400);
 * {@code failureCode = NoResults} on the account-detail operations is treated
 * as "not found" ({@code found = false}); a search with no matches comes back
 * as {@code code = Success} with an empty account list.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserLookupService {

  private static final String CODE_FAILED = "Failed";
  private static final String FAILURE_NO_RESULTS = "NoResults";
  private static final String SORT_ASCENDING = "Ascending";
  private static final String SORT_ON_USER_ID = "UserId";
  private static final int DEFAULT_PAGE_SIZE = 50;
  private static final int PAGE_INDEX = 1; // BCeID SOAP only supports pageIndex = 1

  // Client-facing messages. Raw upstream errors/stack traces are logged
  // server-side only and never returned to callers.
  private static final String UPSTREAM_ERROR_MESSAGE =
      "Unable to complete the request due to an error contacting the user directory service.";
  private static final String NOT_CONFIGURED_MESSAGE =
      "User lookup service is not configured.";

  private final BceidSoapClient soapClient;
  private final BceidProperties properties;

  /** Scenario: an IDIR requester looks up an IDIR user by exact userId. */
  public IdirUserResponse verifyIdirUserByAccountDetail(String userId) {
    checkRequiredCredentials();

    AccountDetailRequest detail = new AccountDetailRequest();
    detail.setOnlineServiceId(properties.getOnlineServiceId());
    detail.setRequesterAccountTypeCode(RequesterAccountTypeCode.Internal.name());
    detail.setRequesterUserGuid(properties.getRequesterUserGuid());
    detail.setUserId(userId);
    detail.setAccountTypeCode(RequesterAccountTypeCode.Internal.name());

    GetAccountDetailRequest request = new GetAccountDetailRequest();
    request.setAccountDetailRequest(detail);

    GetAccountDetailResult result = callGetAccountDetail(request);
    throwIfBusinessFailure(result.getCode(), result.getFailureCode(), result.getMessage(), true);

    IdirUserResponse response = new IdirUserResponse();
    if (isNoResults(result.getCode(), result.getFailureCode())) {
      response.setFound(false);
      response.setUserId(userId);
      return response;
    }

    BceidAccount account = result.getAccount();
    response.setFound(true);
    response.setUserId(value(account.getUserId()));
    response.setGuid(value(account.getGuid()));
    response.setFirstName(firstName(account));
    response.setLastName(lastName(account));
    response.setEmail(email(account));
    return response;
  }

  /** Search IDIR users by firstName / lastName / userId (partial match allowed). */
  public SearchIdirUsersResponse searchIdirUsers(SearchIdirUsersQuery query) {
    checkRequiredCredentials();

    int pageSize = query.getPageSize() == null ? DEFAULT_PAGE_SIZE : query.getPageSize();

    AccountMatch accountMatch = new AccountMatch();
    if (query.getFirstName() != null) {
      accountMatch.setFirstName(
          new MatchProperty(query.getFirstName(), mode(query.getFirstNameMatchMode())));
    }
    if (query.getLastName() != null) {
      accountMatch.setLastName(
          new MatchProperty(query.getLastName(), mode(query.getLastNameMatchMode())));
    }
    if (query.getUserId() != null) {
      accountMatch.setUserId(
          new MatchProperty(query.getUserId(), mode(query.getUserIdMatchMode())));
    }

    InternalAccountSearchRequest inner = new InternalAccountSearchRequest();
    inner.setOnlineServiceId(properties.getOnlineServiceId());
    inner.setRequesterAccountTypeCode(RequesterAccountTypeCode.Internal.name());
    inner.setRequesterUserGuid(properties.getRequesterUserGuid());
    inner.setPagination(new Pagination(String.valueOf(pageSize), String.valueOf(PAGE_INDEX)));
    inner.setSort(new Sort(SORT_ASCENDING, SORT_ON_USER_ID));
    inner.setAccountMatch(accountMatch);

    SearchInternalAccountRequest request = new SearchInternalAccountRequest();
    request.setInternalAccountSearchRequest(inner);

    log.debug("searchIdirUsers (pageSize={}, firstName={}, lastName={}, userId={})",
        pageSize, query.getFirstName(), query.getLastName(), query.getUserId());

    SearchInternalAccountResult result = callSearchInternalAccount(request);
    throwIfBusinessFailure(result.getCode(), result.getFailureCode(), result.getMessage(), false);
    return mapSearchResult(result, pageSize);
  }

  /** Scenario: look up a BCeID business user by userId or userGuid (exact match). */
  public BceidUserResponse verifyBusinessBceidUser(SearchUserParameterType searchUserBy,
      String searchValue) {
    checkRequiredCredentials();

    AccountDetailRequest detail = new AccountDetailRequest();
    detail.setOnlineServiceId(properties.getOnlineServiceId());
    // The configured requester GUID is an Internal/IDIR service identity.
    detail.setRequesterAccountTypeCode(RequesterAccountTypeCode.Internal.name());
    detail.setRequesterUserGuid(properties.getRequesterUserGuid());
    if (searchUserBy == SearchUserParameterType.userGuid) {
      detail.setUserGuid(searchValue);
    } else {
      detail.setUserId(searchValue);
    }
    detail.setAccountTypeCode(RequesterAccountTypeCode.Business.name());

    GetAccountDetailRequest request = new GetAccountDetailRequest();
    request.setAccountDetailRequest(detail);

    GetAccountDetailResult result = callGetAccountDetail(request);
    throwIfBusinessFailure(result.getCode(), result.getFailureCode(), result.getMessage(), true);

    BceidUserResponse response = new BceidUserResponse();
    if (isNoResults(result.getCode(), result.getFailureCode())) {
      response.setFound(false);
      if (searchUserBy == SearchUserParameterType.userGuid) {
        response.setGuid(searchValue);
      } else {
        response.setUserId(searchValue);
      }
      return response;
    }

    BceidAccount account = result.getAccount();
    response.setFound(true);
    response.setUserId(value(account.getUserId()));
    response.setGuid(value(account.getGuid()));
    if (account.getBusiness() != null) {
      response.setBusinessGuid(value(account.getBusiness().getGuid()));
      response.setBusinessLegalName(value(account.getBusiness().getLegalName()));
    }
    response.setFirstName(firstName(account));
    response.setLastName(lastName(account));
    response.setEmail(email(account));
    return response;
  }

  // --- helpers -------------------------------------------------------------

  private void checkRequiredCredentials() {
    if (!StringUtils.hasText(properties.getEndpointUrl())
        || !StringUtils.hasText(properties.getOnlineServiceId())
        || !StringUtils.hasText(properties.getRequesterUserGuid())
        || !StringUtils.hasText(properties.getUsername())
        || !StringUtils.hasText(properties.getPassword())) {
      log.error("BCeID web service is not fully configured "
          + "(url/onlineServiceId/requesterUserGuid/username/password)");
      throw new UpstreamServiceException(NOT_CONFIGURED_MESSAGE);
    }
  }

  private SearchInternalAccountResult callSearchInternalAccount(SearchInternalAccountRequest request) {
    try {
      SearchInternalAccountResult result = soapClient.searchInternalAccount(request);
      if (result == null) {
        log.error("BCeID searchInternalAccount returned an empty response");
        throw new UpstreamServiceException(UPSTREAM_ERROR_MESSAGE);
      }
      return result;
    } catch (UpstreamServiceException e) {
      throw e;
    } catch (Exception e) {
      log.error("BCeID searchInternalAccount call failed", e);
      throw new UpstreamServiceException(UPSTREAM_ERROR_MESSAGE);
    }
  }

  private GetAccountDetailResult callGetAccountDetail(GetAccountDetailRequest request) {
    try {
      GetAccountDetailResult result = soapClient.getAccountDetail(request);
      if (result == null) {
        log.error("BCeID getAccountDetail returned an empty response");
        throw new UpstreamServiceException(UPSTREAM_ERROR_MESSAGE);
      }
      return result;
    } catch (UpstreamServiceException e) {
      throw e;
    } catch (Exception e) {
      log.error("BCeID getAccountDetail call failed", e);
      throw new UpstreamServiceException(UPSTREAM_ERROR_MESSAGE);
    }
  }

  /**
   * Throws a 400 business error when the SOAP code is {@code Failed}, unless the
   * failure is {@code NoResults} and {@code ignoreNoResults} is set (in which
   * case the caller maps it to a not-found response).
   */
  private void throwIfBusinessFailure(String code, String failureCode, String message,
      boolean ignoreNoResults) {
    if (CODE_FAILED.equals(code)) {
      if (ignoreNoResults && FAILURE_NO_RESULTS.equals(failureCode)) {
        return;
      }
      throw new UpstreamBusinessException(code, failureCode, message);
    }
  }

  private boolean isNoResults(String code, String failureCode) {
    return CODE_FAILED.equals(code) && FAILURE_NO_RESULTS.equals(failureCode);
  }

  private SearchIdirUsersResponse mapSearchResult(SearchInternalAccountResult result,
      int requestedPageSize) {
    SearchIdirUsersResponse response = new SearchIdirUsersResponse();
    ResultPagination pagination = result.getPagination();
    int totalItems = pagination == null ? 0 : parseInt(pagination.getTotalItems());
    int responsePageSize = pagination == null ? 0 : parseInt(pagination.getRequestedPageSize());
    response.setTotalItems(totalItems);
    response.setPageSize(responsePageSize > 0 ? responsePageSize : requestedPageSize);

    List<SearchIdirUserResItem> items = new ArrayList<>();
    if (result.getAccountList() != null && result.getAccountList().getBceidAccounts() != null) {
      for (BceidAccount account : result.getAccountList().getBceidAccounts()) {
        SearchIdirUserResItem item = new SearchIdirUserResItem();
        item.setUserId(value(account.getUserId()));
        item.setGuid(value(account.getGuid()));
        item.setFirstName(firstName(account));
        item.setLastName(lastName(account));
        item.setEmail(email(account));
        items.add(item);
      }
    }
    response.setItems(items);
    return response;
  }

  private static String mode(SearchMatchMode mode) {
    return mode == null ? SearchMatchMode.Contains.name() : mode.name();
  }

  private static int parseInt(String value) {
    if (value == null) {
      return 0;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static String value(StringValue wrapped) {
    return wrapped == null ? null : wrapped.getValue();
  }

  private static String firstName(BceidAccount account) {
    if (account.getIndividualIdentity() == null || account.getIndividualIdentity().getName() == null) {
      return null;
    }
    return value(account.getIndividualIdentity().getName().getFirstname());
  }

  private static String lastName(BceidAccount account) {
    if (account.getIndividualIdentity() == null || account.getIndividualIdentity().getName() == null) {
      return null;
    }
    return value(account.getIndividualIdentity().getName().getSurname());
  }

  private static String email(BceidAccount account) {
    return account.getContact() == null ? null : value(account.getContact().getEmail());
  }
}
