package ca.bc.gov.nrs.userlookup.dto;

import lombok.Data;

/** REST response for an IDIR user lookup. */
@Data
public class IdirUserResponse {
  private boolean found;
  private String userId;
  private String guid;
  private String firstName;
  private String lastName;
  private String email;
}
