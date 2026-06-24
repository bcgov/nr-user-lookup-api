package ca.bc.gov.nrs.userlookup.dto;

/**
 * Match behaviour for a search field. The enum constant names are the literal
 * values sent to the BCeID SOAP web service ({@code matchPropertyUsing}).
 */
public enum SearchMatchMode {
  Exact,
  Contains,
  StartsWith
}
