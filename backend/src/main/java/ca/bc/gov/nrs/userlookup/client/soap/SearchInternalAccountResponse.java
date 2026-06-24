package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Root element of a {@code searchInternalAccount} SOAP response. */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "searchInternalAccountResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchInternalAccountResponse {

  @XmlElement(name = "searchInternalAccountResult")
  private SearchInternalAccountResult searchInternalAccountResult;
}
