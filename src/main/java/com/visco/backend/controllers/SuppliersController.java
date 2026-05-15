package com.visco.backend.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.visco.backend.models.dtos.CreateSupplierRequest;
import com.visco.backend.models.dtos.SupplierDTO;
import com.visco.backend.models.dtos.SupplierPerformanceMonthlyDTO;
import com.visco.backend.models.dtos.UpdateSupplierRequest;
import com.visco.backend.services.SupplierService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers", description = "Suppliers endpoints")
public class SuppliersController {

	private final SupplierService supplierService;

	public SuppliersController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	@PostMapping
	public ResponseEntity<SupplierDTO> createSupplier(@RequestBody CreateSupplierRequest request) {
		return ResponseEntity.ok(supplierService.createSupplier(request));
	}

	@GetMapping
	public ResponseEntity<Page<SupplierDTO>> getAllSuppliers(Pageable pageable) {
		return ResponseEntity.ok(supplierService.getAllSuppliers(pageable));
	}

	@GetMapping("/active")
	public ResponseEntity<Page<SupplierDTO>> getActiveSuppliers(Pageable pageable) {
		return ResponseEntity.ok(supplierService.getActiveSuppliers(pageable));
	}

	@GetMapping("/inactive")
	public ResponseEntity<Page<SupplierDTO>> getInactiveSuppliers(Pageable pageable) {
		return ResponseEntity.ok(supplierService.getInactiveSuppliers(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable Long id) {
		return ResponseEntity.ok(supplierService.getSupplierById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<SupplierDTO> updateSupplier(@PathVariable Long id, @Valid @RequestBody UpdateSupplierRequest request) {
		return ResponseEntity.ok(supplierService.updateSupplier(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deactivateSupplier(@PathVariable Long id) {
		supplierService.deactivateSupplier(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/activate")
	public ResponseEntity<Void> activateSupplier(@PathVariable Long id) {
		supplierService.activateSupplier(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/performance")
	public ResponseEntity<List<SupplierPerformanceMonthlyDTO>> getPerformance(
			@RequestParam(defaultValue = "6") int months) {
		return ResponseEntity.ok(supplierService.getSupplierPerformanceChart(months));
	}

}
