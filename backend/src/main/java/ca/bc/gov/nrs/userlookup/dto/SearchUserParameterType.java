package ca.bc.gov.nrs.userlookup.dto;

/**
 * Which property to search a BCeID business user by. The constant names match
 * both the incoming {@code searchUserBy} query value and the BCeID SOAP request
 * field name ({@code userId} / {@code userGuid}), so they are intentionally
 * lower-camelCase rather than the usual uppercase enum style.
 */
public enum SearchUserParameterType {
  userGuid,
  userId
}
