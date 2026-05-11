package com.visco.backend.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.repositories.SupplierRepository;
import com.visco.backend.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SupplierService {

	private final SupplierRepository supplierRepository;
	private final UserRepository userRepository;

	public SupplierService(SupplierRepository supplierRepository, UserRepository userRepository) {
		this.supplierRepository = supplierRepository;
		this.userRepository = userRepository;
	}

	private SupplierDTO createSupplier(Supplier request) {
		Supplier supplier = Supplier.builder()
				.name(request.getName())
				.email(request.getEmail())
				.phoneNumbers(request.getPhoneNumbers())
				.description(request.getDescription())
				.address(request.getAddress())
				.currency(request.getCurrency())
				.active(true)
				.representatives(request.getRepresentatives())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.deletedAt(null)
				.sapCode(request.getSapCode())
				.build();
		return SupplierDTO.fromSupplier(supplierRepository.save(supplier));
	}
}
