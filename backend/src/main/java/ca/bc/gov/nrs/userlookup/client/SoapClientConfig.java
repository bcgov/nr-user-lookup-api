package ca.bc.gov.nrs.userlookup.client;

import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailRequest;
import ca.bc.gov.nrs.userlookup.client.soap.GetAccountDetailResponse;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountRequest;
import ca.bc.gov.nrs.userlookup.client.soap.SearchInternalAccountResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.StringUtils;
import org.springframework.ws.client.core.WebServiceTemplate;

/** Wires the JAXB marshaller and {@link WebServiceTemplate} for BCeID SOAP calls. */
@Configuration
public class SoapClientConfig {

  @Bean
  public Jaxb2Marshaller bceidMarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(
        SearchInternalAccountRequest.class,
        SearchInternalAccountResponse.class,
        GetAccountDetailRequest.class,
        GetAccountDetailResponse.class);
    return marshaller;
  }

  @Bean
  public WebServiceTemplate bceidWebServiceTemplate(Jaxb2Marshaller bceidMarshaller,
      BceidProperties properties) {
    WebServiceTemplate template = new WebServiceTemplate();
    template.setMarshaller(bceidMarshaller);
    template.setUnmarshaller(bceidMarshaller);
    String endpoint = properties.getEndpointUrl();
    if (StringUtils.hasText(endpoint)) {
      template.setDefaultUri(endpoint);
    }
    template.setMessageSender(
        new BasicAuthMessageSender(properties.getUsername(), properties.getPassword()));
    return template;
  }
}
