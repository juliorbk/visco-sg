package com.visco.backend.services;

import java.time.LocalDateTime;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.entities.Currency;
import com.visco.backend.models.entities.Supplier;
import com.visco.backend.repositories.SupplierRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SupplierService {

	private final SupplierRepository supplierRepository;

	public SupplierService(SupplierRepository supplierRepository) {
		this.supplierRepository = supplierRepository;
	}

	// ------ CRUD ------

	// Create Supplier
	public SupplierDTO createSupplier(Supplier request) { // Consider changing 'Supplier' to 'CreateSupplierDTO'
		Supplier supplier = Supplier.builder()
				.name(request.getName())
				.email(request.getEmail())
				.phoneNumbers(request.getPhoneNumbers())
				.description(request.getDescription())
				.address(request.getAddress())
				.currency(request.getCurrency())
				.active(true)
				.representatives(request.getRepresentatives())
				.createdAt(LocalDateTime.now()) // Better handled by @CreatedDate in entity
				.updatedAt(LocalDateTime.now()) // Better handled by @LastModifiedDate in entity
				.deletedAt(null)
				.sapCode(request.getSapCode())
				.build();

		return SupplierDTO.fromSupplier(supplierRepository.save(supplier));
	}

	// Read all Suppliers
	public Page<SupplierDTO> getAllSuppliers(Pageable pageable) {
		return supplierRepository.findAll(pageable).map(SupplierDTO::fromSupplier);
	}

	// Update Supplier

	public SupplierDTO updateSupplier(Long id, Supplier supplier) {
		Supplier existing = supplierRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));

		existing.setName(supplier.getName());
		existing.setEmail(supplier.getEmail());
		existing.setPhoneNumbers(supplier.getPhoneNumbers());
		existing.setDescription(supplier.getDescription());
		existing.setAddress(supplier.getAddress());
		existing.setCurrency(supplier.getCurrency());
		existing.setRepresentatives(supplier.getRepresentatives());
		existing.setSapCode(supplier.getSapCode());
		existing.setUpdatedAt(LocalDateTime.now());

		return SupplierDTO.fromSupplier(supplierRepository.save(existing));
	}

	// Deacativate - Delete Suppplier

	public void deleteSupplier(Long Id) {
		Supplier supplier = supplierRepository.findById(Id)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + Id));

		if (supplier.getActive()) {
			throw new IllegalStateException("Supplier with id: " + Id + " is active, cannot be deleted");
		}
		log.info("Deleting supplier with id: {}", Id);
		supplierRepository.delete(supplier);
	}

	public void deactivateSupplier(Long id) {
		// 1. Properly handle the Optional using orElseThrow
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + id));

		// 2. Safe boolean check (handles null if active is a Boolean object)
		if (Boolean.FALSE.equals(supplier.getActive())) {
			throw new IllegalStateException("Supplier with id: " + id + " is already inactive");
		}

		log.info("Deactivating supplier with id: {}", id);

		// 3. Update state and timestamps
		supplier.setActive(false);
		supplier.setUpdatedAt(LocalDateTime.now());

		supplierRepository.save(supplier);
	}

	public void activateSupplier(Long id) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + id));

		if (Boolean.TRUE.equals(supplier.getActive())) {
			throw new IllegalStateException("Supplier with id: " + id + " is already active");
		}

		log.info("Activating supplier with id: {}", id);

		supplier.setActive(true);
		supplier.setUpdatedAt(LocalDateTime.now());

		supplierRepository.save(supplier);
	}

	public SupplierDTO getSupplierById(Long id) {
		return SupplierDTO.fromSupplier(supplierRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id)));
	}

	public Page<SupplierDTO> getActiveSuppliers(Pageable pageable) {
		return supplierRepository.findByActiveTrue(pageable);
	}

	public Page<SupplierDTO> getInactiveSuppliers(Pageable pageable) {
		return supplierRepository.findByActiveFalse(pageable);
	}

	public Page<SupplierDTO> getSuppliersByCurrency(Currency currency, Pageable pageable) {
		return supplierRepository.findByCurrency(currency, pageable);
	}

}
