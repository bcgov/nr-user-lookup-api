package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of a {@code getAccountDetail} request. Exactly one of {@code userId} or
 * {@code userGuid} is set (the other stays null and is omitted), matching the
 * proxy's dynamic {@code [searchUserBy]: searchValue} behaviour.
 */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"onlineServiceId", "requesterAccountTypeCode",
    "requesterUserGuid", "userId", "userGuid", "accountTypeCode"})
public class AccountDetailRequest {

  @XmlElement(name = "onlineServiceId")
  private String onlineServiceId;

  @XmlElement(name = "requesterAccountTypeCode")
  private String requesterAccountTypeCode;

  @XmlElement(name = "requesterUserGuid")
  private String requesterUserGuid;

  @XmlElement(name = "userId")
  private String userId;

  @XmlElement(name = "userGuid")
  private String userGuid;

  @XmlElement(name = "accountTypeCode")
  private String accountTypeCode;
}
