package com.visco.backend.models.dtos;
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
	private String contactEmail;
	private String contactPhone;

	public static SupplierDTO fromSupplier(Supplier supplier) {
		if (supplier.getId() == null) {
			supplier.setId(0L); // Valor predeterminado para evitar null
		}
		return SupplierDTO.builder()
				.id(supplier.getId())
				.name(supplier.getName())
				.contactEmail(supplier.getEmail())
				.contactPhone(supplier.getContactPhone())
				.build();
	}
}