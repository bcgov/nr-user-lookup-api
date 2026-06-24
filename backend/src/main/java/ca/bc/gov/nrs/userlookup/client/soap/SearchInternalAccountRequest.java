package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Root element for the {@code searchInternalAccount} SOAP operation. */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "searchInternalAccount")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchInternalAccountRequest {

  @XmlElement(name = "internalAccountSearchRequest")
  private InternalAccountSearchRequest internalAccountSearchRequest;
}
