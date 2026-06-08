package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Supplier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
// Response DTO with full supplier details and representative info.
public class SupplierDTO {

  private Long id;
  private String name;
  private String description;
  private String address;
  private String currency;
  private String contactEmail;
  private List<String> phoneNumbers;
  private boolean active;
  private List<RepresentativeInfo> representatives;
  private Long categoryId;
  private String categoryName;

  @Getter
  @Setter
  @Builder
  public static class RepresentativeInfo {

    private Long id;
    private String fullName;
  }

  public static SupplierDTO fromSupplier(Supplier supplier) {
    Long id = supplier.getId() != null ? supplier.getId() : 0L;

    List<String> phones =
      supplier.getPhoneNumbers() != null
        ? new ArrayList<>(supplier.getPhoneNumbers())
        : Collections.emptyList();

    List<RepresentativeInfo> reps =
      supplier.getRepresentatives() != null
        ? supplier
            .getRepresentatives()
            .stream()
            .map(r ->
              RepresentativeInfo.builder()
                .id(r.getId())
                .fullName(r.getFullName())
                .build()
            )
            .collect(Collectors.toList())
        : Collections.emptyList();

    Long categoryId = supplier.getCategory() != null
      ? supplier.getCategory().getId()
      : null;
    String categoryName = supplier.getCategory() != null
      ? supplier.getCategory().getName()
      : null;

    return SupplierDTO.builder()
      .id(id)
      .name(supplier.getName())
      .description(supplier.getDescription())
      .address(supplier.getAddress())
      .currency(
        supplier.getCurrency() != null ? supplier.getCurrency().name() : null
      )
      .contactEmail(supplier.getEmail())
      .phoneNumbers(phones)
      .active(Boolean.TRUE.equals(supplier.getActive()))
      .representatives(reps)
      .categoryId(categoryId)
      .categoryName(categoryName)
      .build();
  }
}
