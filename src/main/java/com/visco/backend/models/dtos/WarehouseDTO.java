package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.Warehouse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
// Response DTO with full warehouse details including responsible user.
public class WarehouseDTO {

	private Long id;
	private String name;
	private String description;
	private String physicalAddress;
	private String sapCenterCode;
	private boolean active;
	private String responsibleUserId; // UUID como String
	private String responsibleUserName;

	public static WarehouseDTO fromEntity(Warehouse w) {
		return WarehouseDTO.builder().id(w.getId()).name(w.getName())
				.description(w.getDescription()).physicalAddress(w.getPhysicalAddress())
				.sapCenterCode(w.getSapCenterCode()).active(w.isActive())
				.responsibleUserId(
						w.getResponsibleUser() != null ? w.getResponsibleUser().getId().toString()
								: null)
				.responsibleUserName(
						w.getResponsibleUser() != null ? w.getResponsibleUser().getName() : null)
				.build();
	}
}
