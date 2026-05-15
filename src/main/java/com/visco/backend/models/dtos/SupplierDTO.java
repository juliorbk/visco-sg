package com.visco.backend.models.dtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


import com.visco.backend.models.entities.Supplier;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SupplierDTO {
	private Long id;
	private String name;
	private String description;
	private String address;
	private String currency;
	private String contactEmail;
	private List<String> phoneNumbers;
	private boolean active;
	private String sapCode;
	private List<RepresentativeInfo> representatives;

	@Getter
	@Setter
	@Builder
	public static class RepresentativeInfo {
		private Long id;
		private String fullName;
	}

	public static SupplierDTO fromSupplier(Supplier supplier) {
		if (supplier.getId() == null) {
			supplier.setId(0L);
		}

		List<String> phones = supplier.getPhoneNumbers() != null
				? new ArrayList<>(supplier.getPhoneNumbers())
				: Collections.emptyList();

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