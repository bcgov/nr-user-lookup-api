package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Account-match filters for a search request. Only the non-null filters are
 * marshalled (JAXB omits null elements), mirroring the proxy which only adds
 * the filters the caller supplied.
 */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"userId", "firstName", "lastName"})
public class AccountMatch {

  @XmlElement(name = "userId")
  private MatchProperty userId;

  @XmlElement(name = "firstName")
  private MatchProperty firstName;

  @XmlElement(name = "lastName")
  private MatchProperty lastName;
}
