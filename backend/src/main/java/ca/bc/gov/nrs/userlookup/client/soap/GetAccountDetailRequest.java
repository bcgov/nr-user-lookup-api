package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Root element for the {@code getAccountDetail} SOAP operation. */
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "getAccountDetail")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAccountDetailRequest {

  @XmlElement(name = "accountDetailRequest")
  private AccountDetailRequest accountDetailRequest;
}
