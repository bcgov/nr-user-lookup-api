package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BCeID wraps most scalar fields in a nested {@code <value>} element, e.g.
 * {@code <userId><value>jdoe</value></userId>}. This models that wrapper.
 */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class StringValue {

  @XmlElement(name = "value")
  private String value;
}
