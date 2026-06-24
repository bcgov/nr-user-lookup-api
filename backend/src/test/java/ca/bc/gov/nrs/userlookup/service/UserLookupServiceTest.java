package ca.bc.gov.nrs.userlookup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.bc.gov.nrs.userlookup.client.BceidSoapClient;
import ca.bc.gov.nrs.userlookup.client.BceidProperties;
import ca.bc.gov.nrs.userlookup.client.soap.AccountList;
import ca.bc.gov.nrs.userlookup.client.soap.BceidAccount;
import ca.bc.gov.nrs.userlookup.client.soap.Contact;
import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailResult;
import ca.bc.gov.nrs.userlookup.client.soap.IndividualIdentity;
import ca.bc.gov.nrs.userlookup.client.soap.PersonName;
import ca.bc.gov.nrs.userlookup.client.soap.ResultPagination;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountRequest;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountResult;
import ca.bc.gov.nrs.userlookup.client.soap.StringValue;
import ca.bc.gov.nrs.userlookup.dto.IdirUserResponse;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersQuery;
import ca.bc.gov.nrs.userlookup.dto.SearchIdirUsersResponse;
import ca.bc.gov.nrs.userlookup.dto.SearchMatchMode;
import ca.bc.gov.nrs.userlookup.exception.UpstreamBusinessException;
import ca.bc.gov.nrs.userlookup.exception.UpstreamServiceException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserLookupServiceTest {

  private static final String REQUESTER_GUID = "12345678901234567890123456789012";

  private final BceidSoapClient soapClient = mock(BceidSoapClient.class);
  private UserLookupService service;

  @BeforeEach
  void setUp() {
    BceidProperties properties = new BceidProperties();
    properties.setUrl("https://bceid.example/BCeIDService.asmx");
    properties.setOnlineServiceId("svc-1");
    properties.setRequesterUserGuid(REQUESTER_GUID);
    properties.setUsername("user");
    properties.setPassword("pass");
    service = new UserLookupService(soapClient, properties);
  }

  private static StringValue sv(String value) {
    StringValue v = new StringValue();
    v.setValue(value);
    return v;
  }

  private static BceidAccount account() {
    BceidAccount account = new BceidAccount();
    account.setUserId(sv("jdoe"));
    account.setGuid(sv("guid1"));
    PersonName name = new PersonName();
    name.setFirstname(sv("John"));
    name.setSurname(sv("Doe"));
    IndividualIdentity identity = new IndividualIdentity();
    identity.setName(name);
    account.setIndividualIdentity(identity);
    Contact contact = new Contact();
    contact.setEmail(sv("john.doe@example.com"));
    account.setContact(contact);
    return account;
  }

  @Test
  void searchMapsSingleAccount() {
    SearchInternalAccountResult result = new SearchInternalAccountResult();
    result.setCode("Success");
    ResultPagination pagination = new ResultPagination();
    pagination.setTotalItems("1");
    pagination.setRequestedPageSize("5");
    result.setPagination(pagination);
    AccountList list = new AccountList();
    list.setBceidAccounts(List.of(account()));
    result.setAccountList(list);
    when(soapClient.searchInternalAccount(any())).thenReturn(result);

    SearchIdirUsersQuery query = new SearchIdirUsersQuery();
    query.setFirstName("John");
    query.setPageSize(5);

    SearchIdirUsersResponse response = service.searchIdirUsers(query);

    assertThat(response.getTotalItems()).isEqualTo(1);
    assertThat(response.getPageSize()).isEqualTo(5);
    assertThat(response.getItems()).hasSize(1);
    assertThat(response.getItems().get(0).getUserId()).isEqualTo("jdoe");
    assertThat(response.getItems().get(0).getEmail()).isEqualTo("john.doe@example.com");
  }

  @Test
  void searchDefaultsPageSizeTo50AndPageIndexTo1() {
    SearchInternalAccountResult result = new SearchInternalAccountResult();
    result.setCode("Success");
    when(soapClient.searchInternalAccount(any())).thenReturn(result);

    SearchIdirUsersQuery query = new SearchIdirUsersQuery();
    query.setUserId("jdoe");

    service.searchIdirUsers(query);

    ArgumentCaptor<SearchInternalAccountRequest> captor =
        ArgumentCaptor.forClass(SearchInternalAccountRequest.class);
    org.mockito.Mockito.verify(soapClient).searchInternalAccount(captor.capture());
    var inner = captor.getValue().getInternalAccountSearchRequest();
    assertThat(inner.getPagination().getPageSizeMaximum()).isEqualTo("50");
    assertThat(inner.getPagination().getPageIndex()).isEqualTo("1");
    // requester GUID comes from configuration, not from the caller
    assertThat(inner.getRequesterUserGuid()).isEqualTo(REQUESTER_GUID);
  }

  @Test
  void searchDefaultsMatchModeToContains() {
    SearchInternalAccountResult result = new SearchInternalAccountResult();
    result.setCode("Success");
    when(soapClient.searchInternalAccount(any())).thenReturn(result);

    SearchIdirUsersQuery query = new SearchIdirUsersQuery();
    query.setFirstName("John"); // no match mode supplied

    service.searchIdirUsers(query);

    ArgumentCaptor<SearchInternalAccountRequest> captor =
        ArgumentCaptor.forClass(SearchInternalAccountRequest.class);
    org.mockito.Mockito.verify(soapClient).searchInternalAccount(captor.capture());
    var match = captor.getValue().getInternalAccountSearchRequest().getAccountMatch();
    assertThat(match.getFirstName().getMatchPropertyUsing())
        .isEqualTo(SearchMatchMode.Contains.name());
  }

  @Test
  void searchBusinessFailureThrows400() {
    SearchInternalAccountResult result = new SearchInternalAccountResult();
    result.setCode("Failed");
    result.setFailureCode("VoidUser");
    result.setMessage("requester not allowed");
    when(soapClient.searchInternalAccount(any())).thenReturn(result);

    SearchIdirUsersQuery query = new SearchIdirUsersQuery();
    query.setFirstName("John");

    assertThatThrownBy(() -> service.searchIdirUsers(query))
        .isInstanceOf(UpstreamBusinessException.class)
        .hasMessageContaining("requester not allowed");
  }

  @Test
  void searchTransportErrorThrows500() {
    when(soapClient.searchInternalAccount(any())).thenThrow(new RuntimeException("connection reset"));

    SearchIdirUsersQuery query = new SearchIdirUsersQuery();
    query.setFirstName("John");

    assertThatThrownBy(() -> service.searchIdirUsers(query))
        .isInstanceOf(UpstreamServiceException.class)
        .hasMessageContaining("user directory service");
  }

  @Test
  void accountDetailNoResultsReturnsNotFound() {
    GetAccountDetailResult result = new GetAccountDetailResult();
    result.setCode("Failed");
    result.setFailureCode("NoResults");
    when(soapClient.getAccountDetail(any())).thenReturn(result);

    IdirUserResponse response =
        service.verifyIdirUserByAccountDetail("jdoe");

    assertThat(response.isFound()).isFalse();
    assertThat(response.getUserId()).isEqualTo("jdoe");
  }

  @Test
  void accountDetailFoundMapsAccount() {
    GetAccountDetailResult result = new GetAccountDetailResult();
    result.setCode("Success");
    result.setAccount(account());
    when(soapClient.getAccountDetail(any())).thenReturn(result);

    IdirUserResponse response =
        service.verifyIdirUserByAccountDetail("jdoe");

    assertThat(response.isFound()).isTrue();
    assertThat(response.getFirstName()).isEqualTo("John");
    assertThat(response.getLastName()).isEqualTo("Doe");
  }

  @Test
  void missingCredentialsThrows500() {
    BceidProperties empty = new BceidProperties();
    UserLookupService bare = new UserLookupService(soapClient, empty);
    SearchIdirUsersQuery query = new SearchIdirUsersQuery();
    query.setFirstName("John");

    assertThatThrownBy(() -> bare.searchIdirUsers(query))
        .isInstanceOf(UpstreamServiceException.class)
        .hasMessageContaining("not configured");
  }
}
