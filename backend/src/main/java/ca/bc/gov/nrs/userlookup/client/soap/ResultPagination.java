package ca.bc.gov.nrs.userlookup.client.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pagination metadata returned in a search result. */
@Getter
@Setter
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class ResultPagination {

  @XmlElement(name = "totalItems")
  private String totalItems;

  @XmlElement(name = "totalVirtualItems")
  private String totalVirtualItems;

  @XmlElement(name = "requestedPageSize")
  private String requestedPageSize;

  @XmlElement(name = "requestedPageIndex")
  private String requestedPageIndex;
}
