package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * List of accounts returned by a search. JAXB binds repeated
 * {@code <BCeIDAccount>} elements into the list (and a single occurrence into a
 * one-element list), so the single-vs-array normalization the proxy did by hand
 * is handled for us.
 */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountList {

  @XmlElement(name = "BCeIDAccount")
  private List<BceidAccount> bceidAccounts;
}
