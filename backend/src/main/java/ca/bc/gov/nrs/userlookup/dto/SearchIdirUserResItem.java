package ca.bc.gov.nrs.userlookup.dto;

import lombok.Data;

/** A single matched IDIR user in a search response. */
@Data
public class SearchIdirUserResItem {
  private String userId;
  private String guid;
  private String firstName;
  private String lastName;
  private String email;
}
