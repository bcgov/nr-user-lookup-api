package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A BCeID account record. Used for both IDIR (internal) and BCeID business
 * lookups; {@link #business} is only populated for business accounts.
 */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class BceidAccount {

  @XmlElement(name = "userId")
  private StringValue userId;

  @XmlElement(name = "guid")
  private StringValue guid;

  @XmlElement(name = "individualIdentity")
  private IndividualIdentity individualIdentity;

  @XmlElement(name = "contact")
  private Contact contact;

  @XmlElement(name = "business")
  private Business business;
}
