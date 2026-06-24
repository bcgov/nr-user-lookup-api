package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Individual identity block of a BCeID account. */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class IndividualIdentity {

  @XmlElement(name = "name")
  private PersonName name;
}
