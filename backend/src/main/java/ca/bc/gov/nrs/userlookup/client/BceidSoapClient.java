package ca.bc.gov.nrs.userlookup.client;

import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailRequest;
import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailResponse;
import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailResult;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountRequest;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountResponse;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;

/**
 * Thin wrapper over {@link WebServiceTemplate} for the two BCeID SOAP operations
 * this service uses. Returns the inner SOAP result payloads; transport/SOAP
 * exceptions propagate to the caller (the service layer translates them).
 */
@Component
@RequiredArgsConstructor
public class BceidSoapClient {

  private final WebServiceTemplate bceidWebServiceTemplate;
  private final BceidProperties properties;

  public SearchInternalAccountResult searchInternalAccount(SearchInternalAccountRequest request) {
    SearchInternalAccountResponse response = (SearchInternalAccountResponse) bceidWebServiceTemplate
        .marshalSendAndReceive(properties.getEndpointUrl(), request,
            soapAction("searchInternalAccount"));
    return response == null ? null : response.getSearchInternalAccountResult();
  }

  public GetAccountDetailResult getAccountDetail(GetAccountDetailRequest request) {
    GetAccountDetailResponse response = (GetAccountDetailResponse) bceidWebServiceTemplate
        .marshalSendAndReceive(properties.getEndpointUrl(), request,
            soapAction("getAccountDetail"));
    return response == null ? null : response.getGetAccountDetailResult();
  }

  private WebServiceMessageCallback soapAction(String operation) {
    String prefix = properties.getSoapActionPrefix() == null ? "" : properties.getSoapActionPrefix();
    return new SoapActionCallback(prefix + operation);
  }
}
