package ca.bc.gov.nrs.userlookup.dto;

import ca.bc.gov.nrs.userlookup.validation.AtLeastOneOf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Query parameters for the IDIR user search. At least one of firstName,
 * lastName or userId must be supplied. Match modes default to
 * {@link SearchMatchMode#Contains} when their field is present (applied in the
 * service). Page size defaults to 50.
 */
@Data
@AtLeastOneOf(fields = {"firstName", "lastName", "userId"},
    message = "At least one of firstName, lastName, or userId must be provided.")
public class SearchIdirUsersQuery {

  @Schema(description = "IDIR first name search value.", maxLength = 50)
  @Size(max = 50)
  private String firstName;

  @Schema(description = "IDIR last name search value.", maxLength = 50)
  @Size(max = 50)
  private String lastName;

  @Schema(description = "IDIR user ID search value.", maxLength = 20)
  @Size(max = 20)
  private String userId;

  @Schema(description = "Match behaviour for firstName. Defaults to Contains.")
  private SearchMatchMode firstNameMatchMode;

  @Schema(description = "Match behaviour for lastName. Defaults to Contains.")
  private SearchMatchMode lastNameMatchMode;

  @Schema(description = "Match behaviour for userId. Defaults to Contains.")
  private SearchMatchMode userIdMatchMode;

  @Schema(description = "Requested page size. Defaults to 50.", defaultValue = "50")
  @Min(1)
  private Integer pageSize;
}
