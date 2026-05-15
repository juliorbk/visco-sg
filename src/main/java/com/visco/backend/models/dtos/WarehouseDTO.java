package com.visco.backend.models.dtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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



		Set<Long> locationIds = 

		List<RepresentativeInfo> reps = supplier.getRepresentatives() != null
				? supplier.getRepresentatives().stream()
						.map(r -> RepresentativeInfo.builder().id(r.getId()).fullName(r.getFullName()).build())
						.collect(Collectors.toList())
				: Collections.emptyList();

		return SupplierDTO.builder()
				.id(supplier.getId())
				.name(supplier.getName())
				.description(supplier.getDescription())
				.address(supplier.getAddress())
				.currency(supplier.getCurrency() != null ? supplier.getCurrency().name() : null)
				.contactEmail(supplier.getEmail())
				.phoneNumbers(phones)
				.active(Boolean.TRUE.equals(supplier.getActive()))
				.sapCode(supplier.getSapCode())
				.representatives(reps)
				.build();
	}
}