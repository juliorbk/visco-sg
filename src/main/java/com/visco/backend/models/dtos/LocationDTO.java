package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Location;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LocationDTO {

  private Long id;
  private String code;
  private boolean active;
  private Long warehouseId;
  private String warehouseName;

  public static LocationDTO fromEntity(Location l) {
    return LocationDTO.builder()
      .id(l.getId())
      .code(l.getCode())
      .active(l.getActive())
      .warehouseId(l.getWarehouse() != null ? l.getWarehouse().getId() : null)
      .warehouseName(
        l.getWarehouse() != null ? l.getWarehouse().getName() : null
      )
      .build();
  }
}
