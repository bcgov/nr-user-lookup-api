package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Root element of a {@code getAccountDetail} SOAP response. */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "getAccountDetailResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAccountDetailResponse {

  @XmlElement(name = "getAccountDetailResult")
  private GetAccountDetailResult getAccountDetailResult;
}
