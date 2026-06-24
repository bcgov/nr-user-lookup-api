package ca.bc.gov.nrs.userlookup.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** Paginated IDIR user search response. */
@Data
public class SearchIdirUsersResponse {
  private int totalItems;
  private int pageSize;
  private List<SearchIdirUserResItem> items = new ArrayList<>();
}
