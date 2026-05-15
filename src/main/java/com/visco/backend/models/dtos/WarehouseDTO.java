package com.visco.backend.models.dtos;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WarehouseDTO {
	private Long id;
	private String name;
	private String description;
	private String physicalAddress;
	private Long responsibleUserId;
	private Set<Long> locationIds;

}