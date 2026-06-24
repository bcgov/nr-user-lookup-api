package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of a {@code searchInternalAccount} request. */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"onlineServiceId", "requesterAccountTypeCode",
    "requesterUserGuid", "pagination", "sort", "accountMatch"})
public class InternalAccountSearchRequest {

  @XmlElement(name = "onlineServiceId")
  private String onlineServiceId;

  @XmlElement(name = "requesterAccountTypeCode")
  private String requesterAccountTypeCode;

  @XmlElement(name = "requesterUserGuid")
  private String requesterUserGuid;

  @XmlElement(name = "pagination")
  private Pagination pagination;

  @XmlElement(name = "sort")
  private Sort sort;

  @XmlElement(name = "accountMatch")
  private AccountMatch accountMatch;
}
