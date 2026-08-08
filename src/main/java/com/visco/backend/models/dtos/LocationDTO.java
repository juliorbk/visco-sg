package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Location;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
// Response DTO with warehouse location details.
public class LocationDTO {

  private Long id;
  private String code;
  private boolean active;
  private Long warehouseId;
  private String warehouseName;
  private String zone;
  private String aisle;
  private String rack;
  private String level;
  private Integer positionX;
  private Integer positionY;
  private String description;

  public static LocationDTO fromEntity(Location l) {
    return LocationDTO.builder()
      .id(l.getId())
      .code(l.getCode())
      .active(l.getActive())
      .warehouseId(l.getWarehouse() != null ? l.getWarehouse().getId() : null)
      .warehouseName(
        l.getWarehouse() != null ? l.getWarehouse().getName() : null
      )
      .zone(l.getZone())
      .aisle(l.getAisle())
      .rack(l.getRack())
      .level(l.getLevel())
      .positionX(l.getPositionX())
      .positionY(l.getPositionY())
      .description(l.getDescription())
      .build();
  }
}
