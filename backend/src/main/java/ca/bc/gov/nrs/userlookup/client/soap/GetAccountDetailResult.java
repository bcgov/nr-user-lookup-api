package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Result payload of a {@code getAccountDetail} response. */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class GetAccountDetailResult {

  @XmlElement(name = "code")
  private String code;

  @XmlElement(name = "failureCode")
  private String failureCode;

  @XmlElement(name = "message")
  private String message;

  @XmlElement(name = "account")
  private BceidAccount account;
}
