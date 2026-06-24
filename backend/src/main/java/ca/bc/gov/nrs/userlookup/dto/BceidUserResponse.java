package ca.bc.gov.nrs.userlookup.dto;

import lombok.Data;

/** REST response for a BCeID business user lookup. */
@Data
public class BceidUserResponse {
  private boolean found;
  private String userId;
  private String guid;
  private String businessGuid;
  private String businessLegalName;
  private String firstName;
  private String lastName;
  private String email;
}
